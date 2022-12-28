(ns app.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [app.pages.core :as pages]
            [app.pages.index.model]
            [app.pages.index.view]
            [app.routes :refer [routes]]
            [re-pressed.core :as rp]
            [app.layout]
            [app.player :as player]
            [app.auth]
            [clojure.core.async :as a]
            [app.dispatch :as dispatch]
            [zframes.srv :as srv]
            [app.ws :as ws]
            #?(:cljs [app.init])
            #?(:cljs [zframes.xhr :as xhr])))

(defn root []
  (let [m (rf/subscribe [app.pages.index.model/index-page])]
    (fn []
      (app.pages.index.view/view @m))))

(defn process-res [[pos [t o]] gmap]
  (if-let [infra (get gmap pos)]
    (condp = infra
      [:b :r] {[(inc (first pos)) (second pos)] [t o]}
      [:b :l] {[(dec (first pos)) (second pos)] [t o]}
      [:b :u] {[(first pos) (dec (second pos))] [t o]}
      [:b :d] {[(first pos) (inc (second pos))] [t o]}
      )

    {pos [t o]}
    )
  )

(defn get-miners [buildings]
  (reduce
   (fn [acc [pos opts]]
     (if (= :m (first opts))
       (assoc acc pos opts)
       acc))
   {}
   buildings))

(defn spawn-on-miner [miners]
  (reduce
   (fn [acc [[x y] [_ _dir type r]]]
     (assoc acc [(inc x)  y] [type r]))
   {} miners)
  )

(rf/reg-event-db
 ::global
 (fn [db _]
   (let [gmap (:buildings db)
         miners (get-miners gmap)
         spawned (spawn-on-miner miners)]
     (-> db
         (update :res
                 (fn [ress]
                   (reduce (fn [acc r] (merge acc (process-res r gmap))) {} ress)
                   ))
         (update :res merge spawned)))))

(def fps 50)
(def d (/ 40 fps))

(defn move-res [[pos [t o dx dy]] gmap]
  (if-let [infra (get gmap pos)]
    (condp = infra
      [:b :r] {pos [t o (+ dx d) dy]}
      [:b :l] {pos [t o (- dx d) dy]}
      [:b :u] {pos [t o dx (- dy d)]}
      [:b :d] {pos [t o dx (+ dy d)]})
    {pos [t o]}
    )
  )


(rf/reg-event-db
 ::animation
 (fn [db _]
   (let [gmap (:buildings db)]
     (update db :res
             (fn [ress]
               (reduce (fn [acc r] (merge acc (move-res r gmap))) {} ress)
               )))
   ))

(rf/reg-fx
 :interval
 (let [live-intervals (atom {})]
   (fn [{:keys [action id frequency event]}]
     (if (= action :start)
       (swap! live-intervals assoc id (js/setInterval #(rf/dispatch event) frequency))
       (do (js/clearInterval (get @live-intervals id))
           (swap! live-intervals dissoc id))))))


(def resources
  {
   [15 4] [:c :h]
   [21 5] [:c :h]
   [14 10] [:c :h]
   [20 11] [:c :h]
   })

(rf/reg-event-fx
 ::init
 (fn [{db :db} _]
   {:fx [[:interval {:action    :start
                     :id        :global
                     :frequency 1000
                     :event     [::global]}]
         [:interval {:action    :start
                     :id        :tick
                     :frequency (/ 1000 fps)
                     :event     [::animation]}]]

    :db (merge db {:res    resources
                   :player {:position {:x 0 :y 0}}})}))

(defn ^:dev/after-load init []
  (rf/dispatch [::init])
  (rdom/render [root] (.getElementById js/document "root")))
