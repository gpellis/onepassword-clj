(ns com.github.gpellis.onepassword-clj.impl.adapt
  "Internal implementation for adapting raw CLI output maps to namespaced maps."
  (:require
   [clojure.walk :as walk]))

;; Define the base namespace for keywords
(def ^:private base-ns "com.github.gpellis.onepassword-clj")

;; Function to create namespaced keywords
(defn- ns-kw
  "Creates a namespaced keyword string like :com.github.gpellis.onepassword-clj.item/id"
  ([entity k]
   (keyword (str base-ns "." (name entity)) (name k)))
  ([k] ; For top-level or generic keys if needed
   (keyword base-ns (name k))))

;; Recursive namespacing function
(defn- deep-namespace-keys
  "Recursively transforms keys in a map/vector structure to use the entity namespace."
  [entity data]
  (walk/postwalk
    (fn [x]
      (if (map? x)
        (->> x
             (map (fn [[k v]] [(ns-kw entity k) v]))
             (into {}))
        x))
    data))

;; --- Entity-specific Adaptation Functions ---
;; These apply the namespacing. They can be expanded later to handle
;; specific field transformations (e.g., date parsing, structure cleaning) if needed.

(defn adapt-vault [raw-vault-map]
  (deep-namespace-keys :vault raw-vault-map))

(defn adapt-item [raw-item-map]
  ;; Items have nested structures (fields, sections, urls) which deep-namespace handles.
  ;; We might want more specific adaptation here later.
  (deep-namespace-keys :item raw-item-map))

(defn adapt-document [raw-document-map]
  (deep-namespace-keys :document raw-document-map))

(defn adapt-user [raw-user-map]
  (deep-namespace-keys :user raw-user-map))

;; You might add more specific adapters here if `deep-namespace-keys` is too generic
;; for certain complex structures within items (like fields, sections, etc.)
;; For example, if you wanted fields to be `:...item.field/id` instead of `:...item/id`
;; inside the `:...item/fields` vector. For now, `deep-namespace-keys` provides
;; a good starting point.
