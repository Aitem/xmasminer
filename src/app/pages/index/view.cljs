(ns app.pages.index.view
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [app.pages.index.model :as model]))

(def belt-dir {:u "belt-u"
               :d "belt-d"
               :r "belt-r"
               :l "belt-l"
               :ur "belt-ur"
               })
(defn menu
  []
  [:div {:style {:padding-top "20px"}}
   [:label "Name "]
   [:input {:type "text" :on-change #(rf/dispatch [::model/change-name (.. % -target -value)])}]
   ])

(defn buildings-menu
  []
  [:div#buildings-menu
   [:div#buildings-menu-items
    [:div.item {:on-click #(rf/dispatch [::model/select-buildings-menu-item {:id :b :dir :u}])} "B"]
    [:div.item 1]
    [:div.item 1]
    [:div.item 1]]
   ])

(defn init-map
  [object]
  (when object
    (.addEventListener
     (js/document.getElementById "map")
     "contextmenu"
     (fn [event]
       (.preventDefault event)
       (let [map-object (.getBoundingClientRect (js/document.getElementById "map"))]
         (rf/dispatch [::model/remove-building
                       (inc (int (/ (- (.-pageX event) (.-x map-object)) 40))) 
                       (inc (int (/ (- (.-pageY event) (.-y map-object)) 40)))]))))
    (js/document.addEventListener
     "keydown"
     (fn [event]
       (case (.-key event)
         "r" (rf/dispatch-sync [::model/seleted-building-rotate])
         nil)))
    (.addEventListener
     (js/document.getElementById "map")
     "click"
     (fn [event]
       (let [map-object (.getBoundingClientRect (js/document.getElementById "map"))]
         (rf/dispatch [::model/create-seleted-building
                       (inc (int (/ (- (.-pageX event) (.-x map-object)) 40))) 
                       (inc (int (/ (- (.-pageY event) (.-y map-object)) 40)))]))))))

(defn view [{{pos :position} :player
             mines :mines
             buildings :buildings res :res belt :belt :as page}]
  [:div#screen
   [menu]
   [buildings-menu]
   [:span#info (str pos)]
   [:div#map {:ref init-map}
    (for [p (:players page)]
      [:div#player {:key (hash p)
                    :style {:background (:color p)
                            :grid-column (-> p :position :x)
                            :grid-row    (-> p :position :y)}}
       [:div {:style {:color "black"
                      :position "absolute"
                      :text-align "center"
                      :margin-top "-20px"}}
        (:name p)]])

    [:<>
     (map
      (fn [[[x y] [type opts]]]
        [:div {:key (hash (str x y type))
               :class (str "belt " (get belt-dir opts))
               :style {:grid-column x :grid-row    y}}])
      buildings)]
    [:<>
     (map
      (fn [[[x y] [type opts dx dy]]]
        [:div {:key (hash (str x y type))
               :class (str "char char-h")
               :style {:margin-left (str (* 2 dx) "px")
                       :margin-top  (str (* 2 dy) "px")
                       :grid-column x :grid-row    y}}])
      res)]
    [:<>
     (map
      (fn [[[x y] [type opts dx dy]]]
        [:div {:key (hash (str x y type))
               :class (str "char char-h")
               :style {:margin-left (str (* 2 dx) "px")
                       :margin-top  (str (* 2 dy) "px")
                       :grid-column x :grid-row    y}}])
      mines)]
    ]
   ]

  )
