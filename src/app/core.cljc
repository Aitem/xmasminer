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

(defn process-res [[pos res] gmap]
  (if-let [infra (get gmap pos)]
    (condp = infra
      [:b :r] {[(inc (first pos)) (second pos)] res}
      [:b :l] {[(dec (first pos)) (second pos)] res}
      [:b :u] {[(first pos) (dec (second pos))] res}
      [:b :d] {[(first pos) (inc (second pos))] res}
      )

    {pos res}
    )
  )

(rf/reg-event-fx
 ::global
 (fn [{db :db} _]
   (let [gmap (:buildings db)]
     {:db (update db :res
                  (fn [ress]
                    (reduce (fn [acc r] (merge acc (process-res r gmap))) {} ress)
                    ))})))

(rf/reg-event-fx
 ::tick
 (fn [{db :db} _]
   (prn "tick")
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
         #_[:interval {:action    :start
                     :id        :tick
                     :frequency 1000
                     :event     [::tick]}]]

    :db (merge db {:res    resources
                   :player {:position {:x 0 :y 0}}})}))

(defn ^:dev/after-load init []
  (rf/dispatch [::init])
  (rdom/render [root] (.getElementById js/document "root")))
