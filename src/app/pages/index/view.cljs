(ns app.pages.index.view
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [app.pages.index.model :as model]))

(defn menu
  []
  [:div {:style {:padding-top "20px"}}
   [:label "Name "]
   [:input {:type "text" :on-change #(rf/dispatch [::model/change-name (.. % -target -value)])}]
   ])

(defn buildings-menu
  []
  (let [state @(rf/subscribe [::model/buildings-menu])]
    [:div#buildings-menu
     [:div#buildings-menu-items
      (for [item (:items state)]
        [:div.item {:on-click #(rf/dispatch [::model/select-buildings-menu-item item])}
         [:div {:id (:id item) :class (:class item)}]])]]))

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
    (js/document.addEventListener
     "mousemove"
     (fn [event]
       (let [map-object (.getBoundingClientRect (js/document.getElementById "map"))
             item (js/document.getElementById "selected-menu-item")]
         (rf/dispatch [::model/map-cursor
                       (inc (int (/ (- (.-pageX event) (.-x map-object)) 40)))
                       (inc (int (/ (- (.-pageY event) (.-y map-object)) 40)))])
         (when (= 1 (.-buttons event))
           (rf/dispatch [::model/create-seleted-building
                         (inc (int (/ (- (.-pageX event) (.-x map-object)) 40))) 
                         (inc (int (/ (- (.-pageY event) (.-y map-object)) 40)))])))))
    (.addEventListener
     (js/document.getElementById "map")
     "click"
     (fn [event]
       (let [map-object (.getBoundingClientRect (js/document.getElementById "map"))]
         (rf/dispatch [::model/create-seleted-building
                       (inc (int (/ (- (.-pageX event) (.-x map-object)) 40))) 
                       (inc (int (/ (- (.-pageY event) (.-y map-object)) 40)))]))))))


(defn tail-type [t]
  (condp = t
     :b "belt"
     :m "miner"
     t))

(defn building-tail [[type opts]]
  (let [t (tail-type type)]
    (str "t " t " " t "-" (last (str opts)))))

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

    (when (and (:cursor page)
               (:buildings-menu-item page))
      [:div {:class (conj [(:class (:buildings-menu-item page))]
                          (building-tail [(:id (:buildings-menu-item page))
                                          (:dir (:buildings-menu-item page))]))
             :style {:opacity     0.4
                     :grid-column (-> page :cursor :x)
                     :grid-row  (-> page :cursor :y)}}])

    ;; buildings
    [:<>
     (map
      (fn [[[x y] [type opts]]]
        [:div {:key (hash (str x y type))
               :class (building-tail [type opts])
               :style {:grid-column x :grid-row y}}])
      buildings)]

    ;; resources
    [:<>
     (map
      (fn [[[x y] [type opts dx dy]]]
        [:div {:key (hash (str x y type dx dy))
               :class (str "char char-h")
               :style {:margin-left (str (* 2 dx) "px")
                       :margin-top  (str (* 2 dy) "px")
                       :grid-column x :grid-row    y}}])
      res)]

    ;; mines
    [:<>
     (map
      (fn [[[x y] [type opts dx dy]]]
        [:div {:key (hash (str x y type))
               :class (str "char mine-h")
               :style {:grid-column x :grid-row    y}}])
      mines)]

    ]
   ]

  )
