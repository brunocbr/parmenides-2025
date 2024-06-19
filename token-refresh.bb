(ns token-refresh
  (:require [babashka.http-client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str] 
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]))

(defn get-token []
  (let [token-file (io/file "token.json")]
    (if (.exists token-file)
      (json/parse-string (slurp token-file) true)
      nil)))

(defn save-token [token]
  (spit "token.json" (json/generate-string token)))

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

(defn open-url [url]
  (let [os-name (System/getProperty "os.name")
        command (cond
                  (str/includes? os-name "Windows") ["cmd.exe" "/c" "start" url]
                  (str/includes? os-name "Mac") ["open" url]
                  :else ["xdg-open" url])]
    (apply shell command)))

(defn request-authorization [client-id client-secret]
  (let [redirect-uri "urn:ietf:wg:oauth:2.0:oob"
        auth-url (str "https://accounts.google.com/o/oauth2/v2/auth?"
                      "client_id=" client-id
                      "&redirect_uri=" redirect-uri
                      "&response_type=code"
                      "&scope=" (java.net.URLEncoder/encode "https://www.googleapis.com/auth/drive" "UTF-8"))
        _ (println "Open the following URL in a browser and enter the authorization code:")
        _ (println auth-url)
        _ (open-url auth-url)
        code (do (print "Enter authorization code: ") (flush) (read-line))
        token-response (client/post "https://oauth2.googleapis.com/token"
                                    {:form-params {:code code
                                                   :client_id client-id
                                                   :client_secret client-secret
                                                   :redirect_uri redirect-uri
                                                   :grant_type "authorization_code"}})]
    (if (= 200 (:status token-response))
      (json/parse-string (:body token-response) true)
      (do
        (println "Error requesting authorization:" (:body token-response))
        (System/exit 1)))))

(defn token-expired? [token-file token]
  (let [modification-time (fs/last-modified-time token-file)
        modification-epoch (fs/file-time->millis modification-time)
        current-time (System/currentTimeMillis)
        expires-in (* (:expires_in token) 1000)
        expiration-time (+ modification-epoch expires-in)]
    (> current-time expiration-time)))

(defn get-service [client-id client-secret]
  (let [token (get-token)]
    (if (and token (not (token-expired? (io/file "token.json") token)))
      token
      (let [new-token (request-authorization client-id client-secret)]
        (save-token new-token)
        new-token))))

(defn main [args]
  (let [args *command-line-args*]
    (if (not= (count args) 1)
      (do
        (println "Usage: bb token-refresh.clj <credentials-file>")
        (System/exit 1)))

    (let [credentials-file (first args)
          credentials (-> (json/parse-string (slurp credentials-file) true)
                          :installed)
          client-id (:client_id credentials)
          client-secret (:client_secret credentials)
          service (get-service client-id client-secret)]
      (println "Access token retrieved and saved successfully."))))

;; Call the main function with command-line arguments
(apply main *command-line-args*)

