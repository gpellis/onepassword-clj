(ns com.github.gpellis.onepassword-clj.core
  "Provides high-level functions to interact with the 1Password CLI ('op').

   Requires the 'op' CLI to be installed and authenticated separately.
   See README prerequisites.

   Functions return data as namespaced Clojure maps on success,
   or an error map (containing an :error key) on failure."
  (:require
   [com.github.gpellis.onepassword-clj.impl.adapt :as adapt]
   [com.github.gpellis.onepassword-clj.impl.cli :as cli]))

(defn- process-result
  "Checks the result from cli fns. Returns adapted data or the error map."
  [result adapter-fn]
  (if (:error result)
    result
    (if (vector? result)
      (mapv adapter-fn result)
      (adapter-fn result))))

(defn list-vaults
  "Lists all vaults the signed-in user has access to.
   Returns a vector of vault maps or an error map."
  []
  (process-result (cli/list-entity "vaults") adapt/adapt-vault))

(defn get-vault
  "Retrieves details for a specific vault by its name or ID.
   Returns a vault map or an error map."
  [id-or-name]
  {:pre [(string? id-or-name)]}
  (process-result (cli/get-entity "vault" id-or-name) adapt/adapt-vault))

(defn list-items
  "Lists items, optionally filtered.
   Returns a vector of item maps or an error map.

   Options map can contain:
   :vault      (string) - Filter by vault name or ID.
   :categories (string) - Comma-separated list of categories (e.g., \"LOGIN,PASSWORD\").
   :tags       (string) - Comma-separated list of tags.
   :include-archive (boolean) - Include items in the Archive."
  ([] (list-items {}))
  ([{:keys [vault categories tags include-archive]}]
   (let [flags (cond-> []
                 vault           (concat ["--vault" vault])
                 categories      (concat ["--categories" categories])
                 tags            (concat ["--tags" tags])
                 include-archive (conj "--include-archive"))]
     (process-result (apply cli/list-entity "items" flags) adapt/adapt-item))))

(defn get-item
  "Retrieves details for a specific item by its name or ID.
   Returns an item map (including fields, sections etc.) or an error map."
  [id-or-name]
  {:pre [(string? id-or-name)]}
  (process-result (cli/get-entity "item" id-or-name) adapt/adapt-item))

(defn list-documents
  "Lists document items, optionally filtered by vault.
   Returns a vector of document maps or an error map.

   Options map can contain:
   :vault (string) - Filter by vault name or ID."
  ([] (list-documents {}))
  ([{:keys [vault]}]
   (let [flags (cond-> [] vault (concat ["--vault" vault]))]
     (process-result (apply cli/list-entity "documents" flags) adapt/adapt-document))))

(defn get-document
  "Retrieves metadata for a specific document by its name or ID.
   Note: This does *not* return the document content. Use `get-document-content` for that.
   Returns a document map or an error map."
  [id-or-name]
  {:pre [(string? id-or-name)]}
  (process-result (cli/get-entity "document" id-or-name) adapt/adapt-document))

(defn get-document-content
  "Retrieves the raw content of a specific document by its name or ID.
   Returns the content as a string, or an error map."
  [id-or-name]
  {:pre [(string? id-or-name)]}
  (cli/get-document-content id-or-name)) ; No adaptation needed for raw string

(defn read-secret
  "Reads a secret value using a 1Password secret reference
   (e.g., \"op://vault/item/field\").
   Returns the secret value as a string, or an error map."
  [op-reference]
  {:pre [(string? op-reference) (re-matches #"op://.*" op-reference)]}
  (cli/read-secret-val op-reference)) ; No adaptation needed for raw string

(defn whoami
  "Retrieves information about the currently signed-in account and user.
   Returns a user map or an error map."
  []
  (process-result (cli/whoami) adapt/adapt-user))
