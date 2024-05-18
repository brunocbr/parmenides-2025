(require '[clojure.java.io :as io]
         '[selmer.parser :as selmer]
         '[clojure.string :as str]
         '[hiccup.core  :as h]
         '[babashka.tasks :as tasks]
         '[babashka.cli :as cli]
         '[babashka.fs :as fs])

(defn inspect [x]
  (prn "+" x)
  x)

(defn read-yaml-header [file]
  (let [lines (-> file io/file slurp str/split-lines)]
    (->> lines
         (drop 1)
         (take-while #(not= "---" %))
         (str/join "\n")
         (yaml/parse-string))))

(defn read-body [file]
  (let [lines (-> file io/file slurp str/split-lines)]
    (->> lines
         (drop 1)
         (drop-while #(not= "---" %))
         (drop 1)
         (drop-while #(= "" %))
         (str/join "\n"))))

(defn obfuscate-email [email]
  (apply str (map #(format "&#x%x;" (int %)) email)))

(defn obfuscated-email-span [email]
  (h/html [:span {:class "obfuscated-email"
                  :data-email (obfuscate-email email)}
           "✉️"]))


(defn unicode-to-ascii [s]
  (loop [acc ""
         remaining s]
    (if (empty? remaining)
      acc
      (let [c (first remaining)
            ascii (int c)]
        (recur (str acc
                    (cond
                      (and (>= ascii 65) (<= ascii 90)) c  ; A-Z
                      (< ascii 128) c                      ; ASCII characters
                      :else "?"))                          ; Non-ASCII characters
               (rest remaining))))))

(comment (unicode-to-ascii "Café and François"))

(defn render-markdown [file]
  (let [header (read-yaml-header file)
        body   (read-body file)
        author (str (:last_name header) ", " (:first_name header))]
    {:author (unicode-to-ascii author)
     :contents (selmer/render
                "#### {{author}}{% if institution %} ({{institution}}){% endif %}. {{title}}{% if email %} {{obfuscated-email|safe}}{% endif %}\n\n{{body}}\n\n"
                {:title (:title header)
                 :author author
                 :email (:email header)
                 :obfuscated-email (obfuscated-email-span (:email header))
                 :institution (:institution header)
                 :body body})}))

(defn process-markdown [files]
  (->> files
       (map render-markdown)
       (sort-by :author)
       (map :contents)
       (str/join "\n\n")))

(defn render-latex [file]
  (let [header (read-yaml-header file)
        output (tasks/shell {:out :string}
                            (format "pandoc -t latex --template=latex/conf-abstract.latex %s"
                                    (.getAbsolutePath file)))
        author (str (:first_name header) " " (:last_name header))
        index-name (:index_name header)]
    {:author (str (if index-name index-name (:last_name header)) (:last_name header) "!" (:first_name header))
     :contents (:out output)}))

(defn process-latex [files]
  (->> files
       (map render-latex)
       (sort-by :author)
       (map :contents)
       (str/join "\n\n")))


(defn process-files [dir f]
  (let [files (file-seq (io/file dir))]
    (->> files
         (filter (or #(.endsWith (str %) ".md")
                     #(.endsWith (str %) ".txt")))
         (f))))

(def cli-spec
  {:spec
   {:format {:desc "Saída latex ou html"
             :default "html"
             :alias :f}
    :path {:desc "Diretório com fontes de dados .txt"
           :validate fs/directory?
           :require true}}})

(defn -main [args]
  (let [opts (cli/parse-opts args cli-spec)
        path (:path opts)]
    (print 
     (case (:format opts)
       "html" (process-files path process-markdown)
       "latex" (process-files path process-latex)))))

(comment)
(-main *command-line-args*)

(comment
  (cli/parse-opts ["--path" "data"] cli-spec)
  (-main ["--path" "data" "-f" "latex"])

  (process-files "data" process-markdown)
  (process-files "data" process-latex)

  (read-yaml-header "data/BLConte.txt")
  (read-body "data/BLConte.txt")
  (render-markdown "data/BLConte.txt"))
