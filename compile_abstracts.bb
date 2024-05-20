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

(defn render-markdown [file]
  (let [header (read-yaml-header file)
        body   (read-body file)
        index-name (:index_name header)
        author (str (if index-name
                      index-name
                      (:last_name header))
                    "!" (:first_name header))]
    {:author author
     :contents (selmer/render
                "#### {{author}}{% if institution %} ({{institution}}){% endif %}. {{title}}{% if email %} {{obfuscated-email|safe}}{% endif %}\n\n{{body}}\n\n"
                {:title (:title header)
                 :author (str (:last_name header) ", " (:first_name header))
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

(defn session->str [{:keys [date time]}]
  (when (and date time)
    (str date " @ " time)))

(defn render-latex [file]
  (let [header (read-yaml-header file)
        output (tasks/shell {:out :string}
                            (format "pandoc -t latex --template=latex/conf-abstract.latex %s"
                                    (.getAbsolutePath file)))
        index-name (:index_name header)]
    {:author (str (if index-name index-name (:last_name header)) "!" (:first_name header))
     :session (or (session->str (:session header)) "ZZZZZ")
     :contents (:out output)}))

(defn process-latex [files]
  (->> files
       (map render-latex)
       (sort-by :author)
       (sort-by :session)
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
  (-main ["--path" "abstracts" "-f" "latex"])

  (process-files "abstracts" process-markdown)
  (process-files "abstracts" process-latex)

  (read-yaml-header "abstracts/BLConte.txt")
  (read-body "abstracts/BLConte.txt")
  (render-markdown "abstracts/BLConte.txt"))
