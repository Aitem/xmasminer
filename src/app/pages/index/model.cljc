(ns app.pages.index.model
  (:require [re-frame.core :as rf]
            [zframes.storage :as storage]
            [zframes.routing :as routing]
            [zframes.pages :as pages]))

(def index-page ::index-page)

(def genders ["male" "female"])

(def pt-names
  {"male"   #{"Arnbo" "Ilugaars" "Kuumtoq" "Dulao"
              "Uruazoc" "Jiusave" "Somerham" "Lonwick"
              "Qassiars" "Buenapant" "Yarcester" "Preshurst" "Buxbron"}
   "female" #{"Kullorsa" "Lecona" "Arbornora" "Bricona"
              "Ilulisna" "Siorapa" "Coamora" "Guadora"
              "Dorrial" "Onostone" "Harpstone"}})

(def pt-avatars
  {"male"   #{"char_1_male.png" "char_2_male.png" "char_4_male.png" "char_6_male.png"
             "char_7_male.png" "char_9_male.png" "char_14_male.png"}
   "female" #{"char_3_female.png" "char_5_female.png" "char_8_female.png" "char_10_female.png"
              "char_11_female.png" "char_12_female.png" "char_13_female.png"}})

(rf/reg-sub
 ::practitioner-name
 (fn [db _]
   (:practitioner-name db)))

(rf/reg-event-db
 ::practitioner-name
 (fn [db [_ name]]
   (-> db
       (assoc :practitioner-name name))))


(rf/reg-event-fx
 index-page
 (fn [{db :db} [pid phase params]]))

(rf/reg-event-fx
 ::start-game
 (fn [{db :db} [evid practitioner-name]]
   {:json/fetch {:uri "/Practitioner"
                 :method :post
                 :body {:name [{:given [practitioner-name]}]}
                 :success {:event ::prepare-patients}
                 :req-id evid}}))


(defn mk-patient-batch-req [{id :id :as practitioner} pts]
  (let [pts (vals pts)]
    (loop [free-avatars pt-avatars
           free-names   pt-names
           patients     []
           idx          0]
      (if (= 3 (count patients))
        patients
        (let [pt-gender (rand-nth genders)
              pt-avatar (rand-nth (vec (get free-avatars pt-gender)))
              pt-name   (rand-nth (vec (get free-names pt-gender)))
              pt-req {:request  {:method (if (nth pts idx)
                                           "PUT"
                                           "POST")
                                 :url (if (nth pts idx)
                                        (str "/Patient/" (:id (nth pts idx)))
                                        "/Patient")}
                      :resource {:name                [{:given [pt-name]}]
                                 :balance             30
                                 :health              3
                                 :gender              pt-gender
                                 :generalPractitioner [{:id id :resourceType "Practitioner"}]
                                 :avatar              pt-avatar}}]
          (recur
           (update free-avatars pt-gender disj pt-avatar)
           (update free-names pt-gender disj pt-name)
           (conj patients pt-req)
           (inc idx)))))))

(defn stat-builder [patient stat]

  {:request {:method "post" :url "/Observation"}
   :resource {:subject {:id (:id patient) :resourceType "Patient"}
              :status "final"
              :code {:coding [{:code   stat
                               :system "urn:observation"}]}
              :value {:Quantity {:value (- 1 (rand-int 3))}}}})

(defn mk-patient-stats-request
  [patient]
  [(stat-builder patient "temperature")
   (stat-builder patient "sugar")
   (stat-builder patient "pressure")
   (stat-builder patient "bacteria")
   (stat-builder patient "diarrhea")])

(rf/reg-event-fx
 ::prepare-patients
 (fn [{db :db} [evid {practitioner :data} ]]
   (let [pts (:patients db)]
     {::storage/set {:player practitioner}
      :json/fetch {:uri "/"
                   :method :post
                   :body {:resourceType "Bundle"
                          :entry (mk-patient-batch-req practitioner pts)}
                   :success {:event ::save-init-data}}})))

(rf/reg-event-fx
 ::save-init-data
 (fn [{_db :db} [_evid {resp :data}]]
   (let [pts (->> resp
                  :entry
                  (map :resource))]
     {:json/fetch {:uri "/"
                   :method :post
                   :body {:resourceType "Bundle"
                          :entry (mapcat mk-patient-stats-request pts)}
                   :success {:event ::run-game}}})))

(rf/reg-event-fx
 ::run-game
 (fn [{db :db} [evid _]]
   {::routing/redirect {:ev :app.pages.game.model/index-page}}))

(rf/reg-sub
 index-page
 (fn [db _]
   {}))
