(ns app.pages.game.model
  (:require [re-frame.core :as rf]
            [zframes.routing :as routing]
            [zframes.storage :as storage]
            [zframes.pages :as pages]
            [clojure.string :as str]))

(def index-page ::index-page)
(def init-ap 1)

(rf/reg-event-fx
 index-page
 [(rf/inject-cofx ::storage/get [:player])]
 (fn [{storage :storage  db :db} [pid phase params]]
   {:db (-> db
            (merge storage)
            (assoc :game-step 1)
            (assoc-in [:ap :total]   init-ap)
            (assoc-in [:ap :current] init-ap))
    :json/fetch [{:uri "/Medication"
                  :req-id ::mds
                  :success {:event ::save-aidbox}}
                 {:uri "/Patient"
                  :params {:general-practitioner (get-in storage [:player :id])}
                  :success {:event ::save-patients}
                  :req-id ::pts}]}))


(defn patients-map [resp]
  (->> resp :data :entry (map :resource)
       (reduce (fn [acc pt] (assoc acc (:id pt) pt)) {})
       (into (sorted-map))))



(rf/reg-event-db
 ::save-obs
 (fn [db [_ resp]]
   (let [obs (->> resp
                  :data
                  :entry
                  (map :resource)
                  (group-by #(get-in % [:subject :id])))
         pt (-> db :patients second second)
         pt-ob (get obs (:id pt))]

     (-> db
         (assoc :selected-pt {:pt pt :obs pt-ob })
         (assoc :observations obs)))))

(rf/reg-event-fx
 ::save-aidbox
 (fn [{db :db} [_ resp]]
   (let [med (->> resp :data :entry (map :resource)
                   (reduce (fn [acc v] (assoc acc (:id v) v)) {}))]
     {:db (assoc db :aidbox med)})))

(rf/reg-event-fx
 ::save-patients
 (fn [{db :db} [_ resp]]
   (let [pts (patients-map resp)]
     {:db (assoc db :patients pts)
      :json/fetch {:uri "/Observation"
                   :params {:subject (str/join "," (keys pts))}
                   :success {:event ::save-obs}}})))

(rf/reg-sub ::aidbox (fn [db _] (:aidbox db)))
(rf/reg-sub ::patients (fn [db _] (:patients db)))
(rf/reg-sub ::observations (fn [db _] (:observations db)))
(rf/reg-sub ::game-step (fn [db _] (:game-step db)))
(rf/reg-sub ::aidbox (fn [db _] (:aidbox db)))
(rf/reg-sub ::ap (fn [db _] (:ap db)))

(rf/reg-sub
 index-page
 :<- [::patients]
 :<- [::observations]
 :<- [::aidbox]
 :<- [::game-step]
 :<- [::ap]
 (fn [[pts obs aids gs ap] _]
   {:pts pts
    :obs obs
    :ap ap
    :game-step gs
    :aidbox aids}))

(defn apply-stats [pt effect]
  (merge-with (fn [a b] (max -2 (min 2 (+ a b)))) pt effect))

(rf/reg-event-fx
 ::apply-drug
 (fn [{db :db} [_ pt drug]]
   (if (or (< (:balance pt) (:price drug))
           (get-in pt [:deceased :boolean]))
     {:db db}
     (when (>= (get-in db [:ap :current]) (:action-point drug))
       (let [ap      (:ap db)
             obs     (get-in db [:observations (:id pt)])
             stats   (get-in db [:observations (:id pt)])
             stats   (group-by #(get-in % [:code :coding 0 :code]) stats)
             stats   (reduce-kv (fn [acc k v]
                                  (assoc acc (keyword k) (get-in v [0 :value :Quantity :value])))
                                {} stats)

             result-stats  (apply-stats stats (:effects drug))

             patient (update pt :balance - (:price drug))
             new-obs (reduce-kv
                      (fn [acc k v]
                        (conj acc
                              (-> obs
                                  (->> (filter #(= (name k) (get-in % [:code :coding 0 :code]))))
                                  first
                                  (assoc-in [:value :Quantity :value] v))))
                      []
                      result-stats)]

         {:db (-> db
                  (assoc-in  [:patients (:id pt)] patient)
                  (assoc-in  [:selected-pt :pt] patient)
                  (assoc-in  [:selected-pt :obs] new-obs)
                  (update-in [:ap :current] - (:action-point drug))
                  (assoc-in  [:observations (:id pt)] new-obs))

          :json/fetch {:uri (str "/Patient/" (:id pt))
                       :method :put
                       :body patient}})))))

(defn mk-damage [_]
  {:sugar        (- 1 (rand-int 3))
   :temperature  (- 1 (rand-int 3))
   :pressure     (- 1 (rand-int 3))
   :bacteria     (- 1 (rand-int 3))
   :diarrhea     (- 1 (rand-int 3))})

(defn do-stat-damage [pt obs damage]
  (let [stats   (group-by #(get-in % [:code :coding 0 :code]) obs)
        stats   (reduce-kv (fn [acc k v] (assoc acc (keyword k) (get-in v [0 :value :Quantity :value]))) {} stats)
        result-stats  (apply-stats stats damage)
        dead? (get-in pt [:deceased :boolean])]
    (reduce-kv
     (fn [acc k v]
       (conj acc
             (-> obs
                 (->> (filter #(= (name k) (get-in % [:code :coding 0 :code]))))
                 first
                 (assoc-in [:value :Quantity :value] (if dead? 0 v)))))
     []
     result-stats)))

(defn do-hp-damage [pt obs]
  (let [stats   (group-by #(get-in % [:code :coding 0 :code]) obs)
        stats   (reduce-kv (fn [acc k v] (assoc acc (keyword k) (get-in v [0 :value :Quantity :value]))) {} stats)
        hp-dmg  (reduce-kv
                 (fn [acc k v] (if (or (> v 1) (< v -1)) (inc acc) acc))
                 0 stats)
        hp-dmg  (if (> hp-dmg 0) 1 0)
        new-hp  (max 0 (- (:health pt) hp-dmg))]
    (if (< new-hp 1)
      (-> pt
          (assoc :health 0)
          (assoc :deceased {:boolean true}))
      (assoc pt :health new-hp))))

(rf/reg-event-fx
 ::synk-state
 (fn [_ [_ resources]]
   {:json/fetch {:uri "/" :method :post
                 :body {:resourceType "Bundle"
                        :entry (map
                                (fn [r]
                                  {:request  {:method "PUT" :url  (str "/" (:resourceType r) "/" (:id r))}
                                   :resource r})
                                resources)}}}))

(rf/reg-event-db
 ::select-pt
 (fn [db [_ pt obs]]
   (assoc db :selected-pt {:pt pt :obs obs})))

(rf/reg-sub ::selected-pt (fn [db _] (:selected-pt db)))

(rf/reg-event-db ::select-drug (fn [db [_ pt]] (assoc db :selected-drug pt)))
(rf/reg-sub ::selected-drug (fn [db _] (:selected-drug db)))


(rf/reg-event-fx
 ::next-step
 (fn [{db :db} [_]]
   (let [nstep (inc (:game-step db))
         patients (get-in db [:patients])
         result-obs (reduce-kv
                     (fn [acc k pt]
                       (assoc acc k (do-stat-damage pt (get-in db [:observations (:id pt)]) (mk-damage {}))))
                     {} patients)
         all-died  (= 3 (count (filter (fn [[k p]] (get-in p [:deceased :boolean])) patients)))
         result-pt (reduce-kv
                    (fn [acc k pt]
                      (assoc acc k (do-hp-damage pt (get-in db [:observations (:id pt)]))))
                    {} patients)]
     (merge
      {:db (-> db
               (assoc :ap {:current (min 10 (+ init-ap (:game-step db)))
                           :total   (min 10 (+ init-ap (:game-step db)))})
               (assoc :observations result-obs)
               (assoc :patients     result-pt)
               (update :game-step   inc))
       :dispatch [::synk-state (concat (vals result-obs) (vals result-pt))]}
      (when (or (=  (:game-step db) 10)
                all-died)
        {::routing/redirect {:ev :app.pages.game.end/index-page}})))))
