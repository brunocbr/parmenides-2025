
(require '[babashka.http-client :as client])
(require '[cheshire.core :as json])
(require '[clojure.java.io :as io])
(require '[clj-yaml.core :as yaml])

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

(defn list-subfolders [service folder-id]
  (let [response (client/get (str "https://www.googleapis.com/drive/v3/files")
                             {:query-params {:q (str "'" folder-id "' in parents and trashed = false and mimeType = 'application/vnd.google-apps.folder'")
                                             :spaces "drive"
                                             :fields "nextPageToken, files(id, name)"}
                              :headers {"Authorization" (str "Bearer " (:access_token service))}})]
    (if (= 200 (:status response))
      (json/parse-string (:body response) true)
      (do
        (println "Error listing subfolders:" (:body response))
        (System/exit 1)))))

(defn get-link [service file-id]
  (let [response (client/get (str "https://www.googleapis.com/drive/v3/files/" file-id)
                             {:query-params {:fields "webViewLink"}
                              :headers {"Authorization" (str "Bearer " (:access_token service))}})]
    (if (= 200 (:status response))
      (-> response :body json/parse-string (get "webViewLink"))
      (do
        (println "Error getting link:" (:body response))
        (System/exit 1)))))

(defn -main [& args]
  (if (not= (count args) 2)
    (do
      (println "Usage: bb drive_subfolders.clj <folder-id> <client-secrets-file>")
      (System/exit 1)))

  (let [folder-id (first args)
        client-secrets (json/parse-string (slurp (second args)) true)
        client-id (:client_id client-secrets)
        client-secret (:client_secret client-secrets)
        service (get-service client-id client-secret)
        subfolders (list-subfolders service folder-id)]
    (->> 
     (for [subfolder (:files subfolders)]
       (let [link (get-link service (:id subfolder))]
         {(:name subfolder)
          link}
         ))
     (into [])
     pr-str
     print)))

;; Call the main function with command-line arguments
(apply -main *command-line-args*)


