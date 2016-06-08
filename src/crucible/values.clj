(ns crucible.values
  (:require [clojure.spec :as s]
            [camel-snake-kebab.core :refer [->PascalCase]]
            [crucible.pseudo :as pseudo]))

(s/def ::ref keyword?)
(s/def ::att keyword?)
(s/def ::delimiter string?)
(s/def ::values (s/+ ::value))
(s/def ::index (s/and pos? integer?))

(defmulti value-type ::type)
(defmulti encode-value ::type)

(s/def ::value (s/multi-spec value-type ::type))

(defmethod value-type ::xref [_]
  (s/keys :req [::type ::ref]
          :opt [::att]))

(defmethod encode-value ::xref [{:keys [::ref ::att]}]
  (if att
    {"Fn::GetAtt" [ref att]}
    {"Ref" ref}))

(defmethod value-type ::pseudo [_]
  (s/keys :req [::type ::param]))

(defmethod encode-value ::pseudo [{:keys [::param]}]
  {"Ref" (-> param name ->PascalCase str)})

(defmethod value-type ::string [_]
  (s/keys :req [::type ::value]))

(defmethod encode-value ::string [{:keys [::value]}]
  (str value))

(defmethod value-type ::join [_]
  (s/keys :req [::type ::values]
          :opt [::delimiter]))

(defmethod encode-value ::join [{:keys [::delimiter ::values]}]
  {"Fn::Join" [(or delimiter "") (vec (map encode-value values))]})

(defmethod value-type ::select [_]
  (s/keys :req [::type ::values ::index]))

(defmethod encode-value ::select [{:keys [::index ::values]}]
  {"Fn::Select" [(str index) (vec (map encode-value values))]})

(defn xref
  ([ref]
   {::type ::xref ::ref ref})
  ([ref att]
   {::type ::xref ::ref ref ::att att}))

(defn pseudo [param]
  {::type ::pseudo
   ::param param})

(defn join
  ([values]
   (join "" values))
  ([delimiter values]
   {::type ::join
    ::values values
    ::delimiter delimiter}))

(defn select [index values]
  {::type ::select
   ::index index
   ::values values})
