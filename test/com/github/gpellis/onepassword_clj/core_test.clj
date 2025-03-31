(ns com.github.gpellis.onepassword-clj.core-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [com.github.gpellis.onepassword-clj.core :as op]
   [com.github.gpellis.onepassword-clj.impl.cli :as cli]))

;; --- Mocking Fixture ---
;; Define dynamic vars holding atoms for each mocked function
(def ^:dynamic *mock-cli-list-entity-atom* (atom nil))
(def ^:dynamic *mock-cli-get-entity-atom* (atom nil))
(def ^:dynamic *mock-cli-get-document-content-atom* (atom nil))
(def ^:dynamic *mock-cli-read-secret-val-atom* (atom nil))
(def ^:dynamic *mock-cli-whoami-atom* (atom nil))

(defn mock-cli-fixture [f]
  ;; Bind NEW atoms for each test run to ensure isolation
  (binding [*mock-cli-list-entity-atom* (atom (constantly {:error :mock-not-set :fn :list-entity}))
            *mock-cli-get-entity-atom* (atom (constantly {:error :mock-not-set :fn :get-entity}))
            *mock-cli-get-document-content-atom* (atom (constantly {:error :mock-not-set :fn :get-doc}))
            *mock-cli-read-secret-val-atom* (atom (constantly {:error :mock-not-set :fn :read-secret}))
            *mock-cli-whoami-atom* (atom (constantly {:error :mock-not-set :fn :whoami}))]
    ;; Redefine the target functions to call the *function stored in the bound atom*
    ;; --- FIX: Use single @ ---
    (with-redefs [cli/list-entity (fn [& args] (apply @*mock-cli-list-entity-atom* args))
                  cli/get-entity (fn [& args] (apply @*mock-cli-get-entity-atom* args))
                  cli/get-document-content (fn [& args] (apply @*mock-cli-get-document-content-atom* args))
                  cli/read-secret-val (fn [& args] (apply @*mock-cli-read-secret-val-atom* args))
                  cli/whoami (fn [& args] (apply @*mock-cli-whoami-atom* args))]
      (f))))

;; Use :each fixture
(use-fixtures :each mock-cli-fixture)

;; --- Tests for Core API ---

(deftest list-vaults-test
  (testing "Success"
    ;; Reset the *atom* with the mock function needed for this test
    (reset! *mock-cli-list-entity-atom*
            (fn [et & flags]
              (is (= "vaults" et))
              (is (empty? flags))
              [{:id "v1", :name "Vault 1"}]))
    (let [result (op/list-vaults)]
      (is (vector? result))
      (is (= 1 (count result)))
      (is (= "v1" (:com.github.gpellis.onepassword-clj.vault/id (first result))))
      (is (= "Vault 1" (:com.github.gpellis.onepassword-clj.vault/name (first result))))))

  (testing "Error passthrough"
    (reset! *mock-cli-list-entity-atom* (constantly {:error :cli-execution, :details "..."}))
    (let [result (op/list-vaults)]
      (is (= :cli-execution (:error result))))))

(deftest get-item-test
  (testing "Success"
    (reset! *mock-cli-get-entity-atom*
            (fn [et id]
              (is (= "item" et))
              (is (= "item123" id))
              {:id "item123", :title "My Item", :category "LOGIN"}))
    (let [result (op/get-item "item123")]
      (is (map? result))
      (is (= "item123" (:com.github.gpellis.onepassword-clj.item/id result)))
      (is (= "My Item" (:com.github.gpellis.onepassword-clj.item/title result)))))

  (testing "Error passthrough"
    (reset! *mock-cli-get-entity-atom* (constantly {:error :not-found, :details "..."}))
    (let [result (op/get-item "item123")]
      (is (= :not-found (:error result))))))

(deftest list-items-test
  (testing "Success with options"
    (reset! *mock-cli-list-entity-atom*
            (fn [et & flags]
              (is (= "items" et))
              (is (= ["--vault" "TestVault" "--categories" "LOGIN"] (vec flags)))
              [{:id "i1", :category "LOGIN"}]))
    (let [result (op/list-items {:vault "TestVault" :categories "LOGIN"})]
      (is (vector? result))
      (is (= "i1" (:com.github.gpellis.onepassword-clj.item/id (first result)))))))

(deftest read-secret-test
  (testing "Success"
    (reset! *mock-cli-read-secret-val-atom*
            (fn [ref]
              (is (= "op://v/i/p" ref))
              "secret-value"))
    (let [result (op/read-secret "op://v/i/p")]
      (is (= "secret-value" result))))

  (testing "Error passthrough"
    (reset! *mock-cli-read-secret-val-atom* (constantly {:error :cli-execution, :details "..."}))
    (let [result (op/read-secret "op://v/i/p")]
      (is (= :cli-execution (:error result))))))

(deftest get-document-content-test
  (testing "Success"
    (reset! *mock-cli-get-document-content-atom*
            (fn [id]
              (is (= "doc_abc" id))
              "Document content"))
    (let [result (op/get-document-content "doc_abc")]
      (is (= "Document content" result))))

  (testing "Error passthrough"
    (reset! *mock-cli-get-document-content-atom* (constantly {:error :cli-execution, :details "..."}))
    (let [result (op/get-document-content "doc_abc")]
      (is (= :cli-execution (:error result))))))

;; TODO: Add more tests for other core functions (get-vault, list-docs, get-doc, whoami)
;; following the same pattern (use reset! on the appropriate atom).
