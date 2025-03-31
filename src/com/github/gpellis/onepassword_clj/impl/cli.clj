(ns com.github.gpellis.onepassword-clj.impl.cli
  "Internal implementation for executing the 'op' CLI."
  (:require
   [clojure.data.json :as json]
   [clojure.java.shell :as shell]
   [clojure.string :as str]))

(defn- execute-op-cmd
  "Executes the 'op' command with the given arguments.
  Returns the parsed JSON output on success (exit code 0).
  Returns an error map on failure (non-zero exit or JSON parse error)."
  [args]
  (try
    (let [cmd (into ["op"] args)
          ;; Specify UTF-8 encoding explicitly for robustness
          {:keys [exit out err] :as result} (apply shell/sh (conj cmd :out-enc :utf8 :err-enc :utf8))]
      (if (zero? exit)
        (if (str/blank? out)
          ;; Handle commands that might return empty output successfully
          {}
          (try
            (json/read-str out :key-fn keyword)
            (catch Exception e
              {:error   :json-parsing
               :message (str "Failed to parse JSON output from 'op' command.")
               :details {:cmd        cmd
                         :raw-output out
                         :exception  e}})))
        ;; Non-zero exit code
        {:error   :cli-execution
         :message (str "1Password CLI command failed with exit code " exit ".")
         :details {:cmd    cmd
                   :exit   exit
                   :stderr (str/trim-newline err)}}))
    (catch java.io.IOException e
      ;; Catch error if 'op' command is not found or executable
      (if (str/includes? (.getMessage e) "Cannot run program \"op\"")
        {:error   :cli-not-found
         :message "The 'op' command-line tool was not found on your PATH."
         :details {:args args
                   :exception e}}
        ;; Other IO exception
        {:error :cli-invocation
         :message "Failed to invoke the 'op' command-line tool."
         :details {:args args
                   :exception e}}))
    (catch Exception e
      {:error   :unknown
       :message "An unexpected error occurred while executing the 'op' command."
       :details {:args args
                 :exception e}})))

(defn list-entity
  "Executes 'op <entity> list' with optional flags (e.g., [\"--vault\" vault-id]).
   Entity should be plural (e.g., \"items\", \"vaults\", \"documents\")."
  [entity-type & flags]
  (let [base-args [entity-type "list" "--format" "json" "--iso-timestamps"]
        args (concat base-args flags)]
    (execute-op-cmd args)))

(defn get-entity
  "Executes 'op <entity> get <id-or-name>'."
  [entity-type id-or-name]
  (let [args [entity-type "get" id-or-name "--format" "json" "--iso-timestamps"]]
    (execute-op-cmd args)))

(defn get-document-content
  "Executes 'op document get <id-or-name>' to retrieve raw content."
  [id-or-name]
  (try
    (let [cmd ["op" "document" "get" id-or-name]
          {:keys [exit out err]} (apply shell/sh (conj cmd :out-enc :utf8 :err-enc :utf8))]
      (if (zero? exit)
        out ; Return raw stdout on success
        {:error   :cli-execution
         :message (str "1Password CLI command failed with exit code " exit ".")
         :details {:cmd    cmd
                   :exit   exit
                   :stderr (str/trim-newline err)}}))
    (catch java.io.IOException e
      (if (str/includes? (.getMessage e) "Cannot run program \"op\"")
        {:error   :cli-not-found
         :message "The 'op' command-line tool was not found on your PATH."
         :details {:args ["document" "get" id-or-name]
                   :exception e}}
        {:error :cli-invocation
         :message "Failed to invoke the 'op' command-line tool."
         :details {:args ["document" "get" id-or-name]
                   :exception e}}))
    (catch Exception e
      {:error   :unknown
       :message "An unexpected error occurred while executing the 'op' command."
       :details {:args ["document" "get" id-or-name]
                 :exception e}})))

(defn read-secret-val
  "Executes 'op read <op-reference>'."
  [op-reference]
  (try
    (let [cmd ["op" "read" op-reference]
          {:keys [exit out err]} (apply shell/sh (conj cmd :out-enc :utf8 :err-enc :utf8))]
      (if (zero? exit)
        (str/trim-newline out) ; Return raw stdout (trimmed) on success
        {:error   :cli-execution
         :message (str "1Password CLI command failed with exit code " exit ".")
         :details {:cmd    cmd
                   :exit   exit
                   :stderr (str/trim-newline err)}}))
    (catch java.io.IOException e
      (if (str/includes? (.getMessage e) "Cannot run program \"op\"")
        {:error   :cli-not-found
         :message "The 'op' command-line tool was not found on your PATH."
         :details {:args ["read" op-reference]
                   :exception e}}
        {:error :cli-invocation
         :message "Failed to invoke the 'op' command-line tool."
         :details {:args ["read" op-reference]
                   :exception e}}))
    (catch Exception e
      {:error   :unknown
       :message "An unexpected error occurred while executing the 'op' command."
       :details {:args ["read" op-reference]
                 :exception e}})))

(defn whoami
  "Executes 'op whoami'."
  []
  (execute-op-cmd ["whoami" "--format" "json"]))
