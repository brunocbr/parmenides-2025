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

(defn get-data [file]
  (->> (read-yaml-header file)
       (into {})))

(defn get-sessions []
  (get-data "data/sessions.yml"))

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
                "#### {{author}}{% if institution %} ({{institution}}){% endif %}. {{title}}{% if email %} {{obfuscated-email|safe}}{% endif %}\n\n" ;; {{body}}\n\n
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

(defn render-program-entry [file]
  (let [header (read-yaml-header file)
        {:keys [id order]} (:session header)]
    {:contents (selmer/render
                "{{title}}  \n{{author}}{% if institution %}, {{institution}}{% endif %}"
                {:title (:title header)
                 :author (str (:first_name header) " " (:last_name header))
                 :institution (:institution header)})
     :session-id id
     :session-order order}))

(defn render-program-session [all-entries {:keys [id date time]}]
  (let [session-entries (->> all-entries
                             (filter #(= id (:session-id %)))
                             (sort-by :session-order))]
    (str/join "\n\n"
              (cons (format "# Session %s" id),
                    (for [e session-entries]
                      (:contents e))))))

(defn process-program [files]
  (let [program-entries (->> files (map render-program-entry))
        data (get-sessions)]
    (->> data
         :sessions
         (map #(render-program-session program-entries %))
         (str/join "\n\n"))))

(comment
  (process-program ))


(defn session->str [{:keys [date time order]}]
  (cond
    (and date time order)
    (str date " @ " time " @ " order)
    (and date time)
    (str date " @ " time)))

(defn get-by-id [id m]
  (->> m
       (filter #(= id (:id %)))
       first))

(defn render-latex [{:keys [sessions]} file]
  (let [header (read-yaml-header file)
        session-id (get-in header [:session :id])
        session (get-by-id session-id sessions)
        output (tasks/shell {:out :string}
                            (format "pandoc -t latex --template=templates/conf-abstract.latex -M date=\"%s\" -M time=\"%s\" \"%s\""
                                    (:date session)
                                    (:time session)
                                    (.getAbsolutePath file)))
        index-name (:index_name header)]
    {:author (str (if index-name index-name (:last_name header)) "!" (:first_name header))
     :session session-id
     :order (get-in header [:session :order])
     :contents (:out output)}))

(defn process-latex [files]
  (let [data (get-sessions)]
    (->> files
         (map #(render-latex data %))
         (sort-by :author)
         (sort-by :order)
         (sort-by :session)
         (map :contents)
         (str/join "\n\n"))))

(defn process-author-list [files]
  (->>
   (for [f files]
     (let [h (read-yaml-header f)]
       (str (:first_name h) " " (:last_name h))))
   (str/join "\n")))

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
       "latex" (process-files path process-latex)
       "program" (process-files path process-program)
       "authors" (process-files path process-author-list)))))

;; (if-not (bound? #'*1))
(-main *command-line-args*)

(comment
  (cli/parse-opts ["--path" "data"] cli-spec)
  (-main ["--path" "abstracts" "-f" "latex"])

  (process-files "abstracts" process-program)
  (process-files "abstracts" process-latex)

  (let [data (get-data "data/sessions.yml")]
    (render-latex data (io/file "abstracts/ABraga.md")))

  (read-yaml-header "abstracts/BLConte.md")
  (read-body "abstracts/BLConte.md"))
