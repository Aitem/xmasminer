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

(rf/reg-event-fx
 ::global
 (fn [{db :db} _]
   (prn "global")
   {:db (update-in db [:res :#c_1 0] inc)}))

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


(rf/reg-event-fx
 ::init
 (fn [{db :db} _]

   ()
   {:fx [[:interval {:action    :start
                     :id        :global
                     :frequency 1000
                     :event     [::global]}]
         #_[:interval {:action    :start
                     :id        :tick
                     :frequency 1000
                     :event     [::tick]}]]

    :db (merge db {:res    {:#c_1    [15 4 :h]}
                   :player {:position {:x 10 :y 10}}})}))

(defn ^:dev/after-load init []
  (rf/dispatch [::init])
  (rdom/render [root] (.getElementById js/document "root")))
