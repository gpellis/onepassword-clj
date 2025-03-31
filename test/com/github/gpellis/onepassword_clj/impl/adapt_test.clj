(ns com.github.gpellis.onepassword-clj.impl.adapt-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.github.gpellis.onepassword-clj.impl.adapt :as adapt]))

(deftest adapt-vault-test
  (testing "Adapts a simple vault map"
    (let [raw {:id "v1", :name "Personal", :created_at "2023-01-01T10:00:00Z"}
          adapted (adapt/adapt-vault raw)]
      (is (= :com.github.gpellis.onepassword-clj.vault/id (-> adapted keys first)))
      (is (= "v1" (:com.github.gpellis.onepassword-clj.vault/id adapted)))
      (is (= "Personal" (:com.github.gpellis.onepassword-clj.vault/name adapted)))
      (is (= "2023-01-01T10:00:00Z" (:com.github.gpellis.onepassword-clj.vault/created_at adapted)))))

  (testing "Adapts vault map with nested structures (shouldn't exist but test robustness)"
    (let [raw {:id "v2", :details {:attr "value"}}
          adapted (adapt/adapt-vault raw)]
      (is (= "v2" (:com.github.gpellis.onepassword-clj.vault/id adapted)))
      (is (= {:com.github.gpellis.onepassword-clj.vault/attr "value"}
             (:com.github.gpellis.onepassword-clj.vault/details adapted))))))

(deftest adapt-item-test
  (testing "Adapts a simple item map"
    (let [raw {:id "i1", :title "Test Item", :category "LOGIN"}
          adapted (adapt/adapt-item raw)]
      (is (= :com.github.gpellis.onepassword-clj.item/id (-> adapted keys first)))
      (is (= "i1" (:com.github.gpellis.onepassword-clj.item/id adapted)))
      (is (= "Test Item" (:com.github.gpellis.onepassword-clj.item/title adapted)))
      (is (= "LOGIN" (:com.github.gpellis.onepassword-clj.item/category adapted)))))

  (testing "Adapts an item map with nested fields vector"
    (let [raw {:id "i2", :title "Complex Item"
               :fields [{:id "f1", :label "username", :value "user"}
                        {:id "f2", :label "password", :value "pass"}]}
          adapted (adapt/adapt-item raw)]
      (is (= "i2" (:com.github.gpellis.onepassword-clj.item/id adapted)))
      (let [fields (:com.github.gpellis.onepassword-clj.item/fields adapted)]
        (is (vector? fields))
        (is (= 2 (count fields)))
        (is (= "f1" (:com.github.gpellis.onepassword-clj.item/id (first fields))))
        (is (= "username" (:com.github.gpellis.onepassword-clj.item/label (first fields))))
        (is (= "user" (:com.github.gpellis.onepassword-clj.item/value (first fields)))))))

  (testing "Adapts an item with URLs"
    (let [raw {:id "i3", :urls [{:href "https://example.com", :primary true}]}
          adapted (adapt/adapt-item raw)]
      (let [urls (:com.github.gpellis.onepassword-clj.item/urls adapted)]
        (is (vector? urls))
        (is (= "https://example.com" (:com.github.gpellis.onepassword-clj.item/href (first urls))))
        (is (= true (:com.github.gpellis.onepassword-clj.item/primary (first urls))))))))

(deftest adapt-document-test
  (testing "Adapts a simple document map"
    (let [raw {:id "d1", :title "My Doc", :version 1}
          adapted (adapt/adapt-document raw)]
      (is (= "d1" (:com.github.gpellis.onepassword-clj.document/id adapted)))
      (is (= "My Doc" (:com.github.gpellis.onepassword-clj.document/title adapted)))
      (is (= 1 (:com.github.gpellis.onepassword-clj.document/version adapted))))))

(deftest adapt-user-test
  (testing "Adapts a user map from whoami"
    (let [raw {:url "my.1password.com", :email "a@b.c", :user_uuid "uuid1", :account_uuid "acc_uuid"}
          adapted (adapt/adapt-user raw)]
      (is (= "my.1password.com" (:com.github.gpellis.onepassword-clj.user/url adapted)))
      (is (= "a@b.c" (:com.github.gpellis.onepassword-clj.user/email adapted)))
      (is (= "uuid1" (:com.github.gpellis.onepassword-clj.user/user_uuid adapted)))
      (is (= "acc_uuid" (:com.github.gpellis.onepassword-clj.user/account_uuid adapted))))))
