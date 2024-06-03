
(require '[babashka.http-client :as client])
(require '[cheshire.core :as json])
(require '[clojure.java.io :as io])
(require '[clojure.string :as str])


(defn get-token []
  (let [token-file (io/file "token.json")]
    (if (.exists token-file)
      (json/parse-string (slurp token-file) true)
      (do
        (println "Error: token.json file not found.")
        (System/exit 1)))))

(defn refresh-token [client-id client-secret refresh-token]
  (let [response (client/post "https://oauth2.googleapis.com/token"
                              {:form-params {:client_id client-id
                                             :client_secret client-secret
                                             :refresh_token refresh-token
                                             :grant_type "refresh_token"}})]
    (if (= 200 (:status response))
      (json/parse-string (:body response) true)
      (do
        (println "Error refreshing token:" (:body response))
        (System/exit 1)))))

(defn save-token [token]
  (spit "token.json" (json/generate-string token)))

(defn get-service [client-id client-secret]
  (let [token (get-token)]
    (if (-> token :expires_in (<= 0))
      (let [new-token (refresh-token client-id client-secret (:refresh_token token))]
        (save-token new-token)
        new-token)
      token)))

(defn create-subfolder [service parent-id subfolder-name]
  (let [response (client/post "https://www.googleapis.com/drive/v3/files"
                              {:headers {"Authorization" (str "Bearer " (:access_token service))
                                         "Content-Type" "application/json"}
                               :body (json/generate-string {:name subfolder-name
                                                            :mimeType "application/vnd.google-apps.folder"
                                                            :parents [parent-id]})})]
    (if (= 200 (:status response))
      (println "Created subfolder:" subfolder-name)
      (println "Error creating subfolder:" subfolder-name ":" (:body response)))))

(defn -main [& args]
  (if (not= (count args) 2)
    (do
      (println "Usage: bb create_subfolders.clj <parent-folder-id> <client-secrets-file>")
      (System/exit 1)))

  (let [parent-id (first args)
        client-secrets (json/parse-string (slurp (second args)) true)
        client-id (:client_id client-secrets)
        client-secret (:client_secret client-secrets)
        service (get-service client-id client-secret)
        subfolder-names (line-seq (java.io.BufferedReader. *in*))]
    (doseq [subfolder-name subfolder-names]
      (create-subfolder service parent-id subfolder-name))))

;; Call the main function with command-line arguments
(apply -main *command-line-args*)

