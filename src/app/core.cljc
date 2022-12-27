(ns app.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [app.pages.core :as pages]
            [app.routes :refer [routes]]
            [re-pressed.core :as rp]
            [app.layout]
            [app.player :as player]
            [app.auth]
            [clojure.core.async :as a]
            [app.dispatch :as dispatch]
            [zframes.srv :as srv]
            [app.ws :as ws]
            #?(:cljs [zframes.xhr :as xhr])))

(def response (r/atom nil))

(defn handler [req]
  (reset! response (dispatch/handler req)))

(def srv  (srv/run handler))

(defn root [] @response)


(rf/reg-fx
 :interval
 (let [live-intervals (atom {})]
   (fn [{:keys [action id frequency event]}]
     (if (= action :start)
       (swap! live-intervals assoc id (js/setInterval #(rf/dispatch event) frequency))
       (do (js/clearInterval (get @live-intervals id))
           (swap! live-intervals dissoc id))))))


(rf/reg-event-fx
 ::tick
 (fn [{db :db} _]
   (prn "Hello")
   ))

(rf/reg-event-fx
 ::init
 (fn [{db :db} _]
   {:interval {:action :start
               :id :tick
               :frequency 1000
               :event [::tick]}

    :db (merge db {:player {:position {:x 10 :y 10}}})}))

(defn ^:dev/after-load init []
  (srv)
  (rf/dispatch [::init])

  ;;(rf/dispatch-sync [::xhr/init {:base-url "https://healthmesamurai.edge.aidbox.app"}])

  (rdom/render [root] (.getElementById js/document "root")))


(defonce initialize
  (do 
    (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
    (rf/dispatch
     [::rp/set-keydown-rules
      {;; takes a collection of events followed by key combos that can trigger the event
       :event-keys [
                    [[::player/move-w]
                     [{:keyCode 87}]]
                    [[::player/move-a]
                     [{:keyCode 65}]]
                    [[::player/move-s]
                     [{:keyCode 83}]]
                    [[::player/move-d]
                     [{:keyCode 68}]]

                    ;; Event & key combos 2
                    [;; this event
                     [:some-event-id2]
                     ;; will be triggered if
                     ;; tab is pressed twice in a row
                     [{:keyCode 9} {:keyCode 9}]
                     ]]

       ;; takes a collection of key combos that, if pressed, will clear
       ;; the recorded keys
       :clear-keys
       ;; will clear the previously recorded keys if
       [;; escape
        [{:keyCode 27}]
        ;; or Ctrl+g
        [{:keyCode   71
          :ctrlKey true}]]
       ;; is pressed

       ;; takes a collection of keys that will always be recorded
       ;; (regardless if the user is typing in an input, select, or textarea)
       :always-listen-keys
       ;; will always record if
       [;; enter
        {:keyCode 13}]
       ;; is pressed

       ;; takes a collection of keys that will prevent the default browser
       ;; action when any of those keys are pressed
       ;; (note: this is only available to keydown)
       :prevent-default-keys
       ;; will prevent the browser default action if
       [;; Ctrl+g
        {:keyCode   71
         :ctrlKey true}]
       ;; is pressed
       }])))
