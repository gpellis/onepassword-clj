(ns com.github.gpellis.onepassword-clj.impl.cli-test
  (:require
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [com.github.gpellis.onepassword-clj.impl.cli :as cli]))

;; --- Mocking Fixture ---
;; Store an atom inside the dynamic var
(def ^:dynamic *mock-sh-response-atom* (atom nil))

(defn mock-sh [& args]
  ;; Deref the var to get the atom, then deref the atom to get the value
  ;; --- FIX: Use single @ ---
  (if @*mock-sh-response-atom*
    @*mock-sh-response-atom*
    (throw (IllegalStateException. "No mock response set for shell/sh"))))

(defn mock-sh-fixture [f]
  (binding [*mock-sh-response-atom* (atom nil)] ; Bind a *new* atom for each test run
    (with-redefs [shell/sh mock-sh]
      (f))))

;; Use :each fixture to ensure rebinding and isolation for each test
(use-fixtures :each mock-sh-fixture)

;; --- Tests ---

(deftest list-entity-test
  (testing "Successful list"
    ;; Reset the atom held by the var
    (reset! *mock-sh-response-atom* {:exit 0 :out "[{\"id\":\"v1\"}, {\"id\":\"v2\"}]" :err ""})
    (let [result (cli/list-entity "vaults")]
      (is (vector? result))
      (is (= 2 (count result)))
      (is (= {:id "v1"} (first result)))))

  (testing "List with flags"
    (reset! *mock-sh-response-atom* {:exit 0 :out "[{\"id\":\"i1\"}]" :err ""})
    (let [result (cli/list-entity "items" "--vault" "TestVault")]
      (is (vector? result))
      (is (= {:id "i1"} (first result)))))

  (testing "CLI execution error"
    (reset! *mock-sh-response-atom* {:exit 1 :out "" :err "op: error message"})
    (let [result (cli/list-entity "vaults")]
      (is (= :cli-execution (:error result)))
      (is (= 1 (get-in result [:details :exit])))
      (is (= "op: error message" (get-in result [:details :stderr])))))

  (testing "JSON parsing error"
    (reset! *mock-sh-response-atom* {:exit 0 :out "[{\"id\":\"v1\"" :err ""}) ; Invalid JSON
    (let [result (cli/list-entity "vaults")]
      (is (= :json-parsing (:error result)))
      (is (= "[{\"id\":\"v1\"" (get-in result [:details :raw-output]))))))

(deftest get-entity-test
  (testing "Successful get"
    (reset! *mock-sh-response-atom* {:exit 0 :out "{\"id\":\"i1\", \"title\":\"Item 1\"}" :err ""})
    (let [result (cli/get-entity "item" "i1")]
      (is (map? result))
      (is (= {:id "i1" :title "Item 1"} result))))

  (testing "Get non-existent (returns error from op)"
    (reset! *mock-sh-response-atom* {:exit 1 :out "" :err "op: item not found"})
    (let [result (cli/get-entity "item" "non-existent")]
      (is (= :cli-execution (:error result)))
      (is (= "op: item not found" (get-in result [:details :stderr]))))))

(deftest get-document-content-test
  (testing "Successful get document content"
    (reset! *mock-sh-response-atom* {:exit 0 :out "This is the doc content.\n" :err ""})
    (let [result (cli/get-document-content "doc1")]
      (is (= "This is the doc content.\n" result))))

  (testing "Get document content error"
    (reset! *mock-sh-response-atom* {:exit 1 :out "" :err "op: doc not found"})
    (let [result (cli/get-document-content "doc_xyz")]
      (is (= :cli-execution (:error result)))
      (is (= "op: doc not found" (get-in result [:details :stderr]))))))

(deftest read-secret-val-test
  (testing "Successful read"
    (reset! *mock-sh-response-atom* {:exit 0 :out "mysecret\n" :err ""}) ; Note newline
    (let [result (cli/read-secret-val "op://v/i/p")]
      (is (= "mysecret" result)))) ; Expect newline trimmed

  (testing "Read secret error"
    (reset! *mock-sh-response-atom* {:exit 1 :out "" :err "op: reference not found"})
    (let [result (cli/read-secret-val "op://v/i/invalid")]
      (is (= :cli-execution (:error result)))
      (is (= "op: reference not found" (get-in result [:details :stderr]))))))

(deftest whoami-test
  (testing "Successful whoami"
    (reset! *mock-sh-response-atom* {:exit 0 :out "{\"url\":\"my.1p.com\", \"email\":\"a@b.c\"}" :err ""})
    (let [result (cli/whoami)]
      (is (= {:url "my.1p.com" :email "a@b.c"} result))))

  (testing "Whoami error (e.g., not signed in)"
    (reset! *mock-sh-response-atom* {:exit 1 :out "" :err "op: not signed in"})
    (let [result (cli/whoami)]
      (is (= :cli-execution (:error result)))
      (is (= "op: not signed in" (get-in result [:details :stderr]))))))
