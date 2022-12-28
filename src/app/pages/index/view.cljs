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
       (rf/dispatch [::model/remove-building])))
    (js/document.addEventListener
     "keydown"
     (fn [event]
       (case (.-key event)
         "r" (rf/dispatch-sync [::model/seleted-building-rotate])
         nil)))
    (js/document.addEventListener
     "mousemove"
     (fn [event]
       (let [map-object (.getBoundingClientRect (js/document.getElementById "map"))]
         (rf/dispatch [::model/map-cursor
                       (int (/ (- (.-x event) (.-x map-object)) 40))
                       (int (/ (- (.-y event) (.-y map-object)) 40))])
         (when (= 1 (.-buttons event))
           (rf/dispatch [::model/create-seleted-building]))
         (when (= 2 (.-buttons event))
           (rf/dispatch [::model/remove-building]))
         )))
    (.addEventListener
     (js/document.getElementById "map")
     "click"
     (fn [event]
       (rf/dispatch [::model/create-seleted-building])))))


(defn tile-type [t]
  (condp = t
     :b "belt"
     :m "miner"
     :h "hub"
     :q "quest"

     t))

(defn building-tile [[type opts]]
  (let [t (tile-type type)]
    (str "t " t " " t "-" (last (str opts)))))

(defn add-content [block content]
  (cond-> block
    content (conj block content)))

(defn render-generic-box
  ([box-spec content]
   (let [box-width (:width box-spec)
         box-height (:height box-spec)
         vp-x-pos (:vp-x box-spec)
         vp-y-pos (:vp-y box-spec)]
     (-> [:div.box {:key (str "box-" vp-x-pos "-" vp-y-pos "-" box-width "-" box-height)
                    :style {:grid-column-start (inc vp-x-pos)
                            :grid-column-end (+ vp-x-pos box-width)
                            :grid-row-start (inc vp-y-pos)
                            :grid-row-end (+ vp-y-pos box-height)}}]
         (add-content content))))
  ([box-spec]
   (render-generic-box box-spec nil)))


(defn render-tile
  ([box-spec content]
   (render-generic-box box-spec
                       (add-content
                        [:div.tile {:class (:tile box-spec)}]
                        content)))
  ([box-spec]
   (render-tile box-spec nil)))

(defn render-colorbox
  ([box-spec content]
   (render-generic-box box-spec
                       (add-content
                        [:div.colorbox {:style {:background (:color box-spec)}}]
                        content)))
  ([box-spec]
   (render-colorbox box-spec nil)))

(defn render-player [player-data]
  (render-colorbox {:width 1
                    :height 1
                    :vp-x (:vp-x player-data)
                    :vp-y (:vp-y player-data)}
                   [:div.player {:style {:color "black"
                                         :position "absolute"
                                         :text-align "center"
                                         :margin-top "-20px"}}]))


(defn view [{{pos :position} :player
             mines :mines
             buildings :buildings res :res belt :belt :as page}]
  (let [vp-x (get-in page [:viewport :x])
        vp-y (get-in page [:viewport :y])
        vp-h (get-in page [:viewport :h])
        vp-w (get-in page [:viewport :w])]
    [:div#screen
     [menu]
     [buildings-menu]
     [:span#info (str pos)]
     [:div#map {:ref init-map}
      (for [p (:players page)
            :let [vp-px (- (get-in p [:position :x]) vp-x)
                  vp-py (- (get-in p [:position :y]) vp-y)]
            :when (and (>= vp-px 0)
                       (>= vp-py 0)
                       (< vp-px vp-w)
                       (< vp-py vp-h))]
        [:div#player {:key (hash p)
                      :style {:background (:color p)
                              :grid-column (inc vp-px)
                              :grid-row (inc vp-py)}}
         [:div {:style {:color "black"
                        :position "absolute"
                        :text-align "center"
                        :margin-top "-20px"}}
          (:name p)]])

      (when (and (:cursor page)
                 (:buildings-menu-item page))
        (let [item-id (-> page :buildings-menu-item :id)
              item-x  (-> page :cursor :x)
              item-y  (-> page :cursor :y)]
          (if (model/allow-building-create? page (:buildings-menu-item page))
            [:div {:class (conj [(:class (:buildings-menu-item page))]
                                (building-tile [(:id (:buildings-menu-item page))
                                                (:dir (:buildings-menu-item page))]))
                   :style {:opacity     0.4
                           :grid-column item-x
                           :grid-row  item-y}}]
            [:div {:class (conj [(:class (:buildings-menu-item page))]
                                (building-tile [(:id (:buildings-menu-item page))
                                                (:dir (:buildings-menu-item page))]))
                   :style {:opacity     0.4
                           :background "red"
                           :grid-column item-x
                           :grid-row  item-y}}])))

      ;; buildings
      [:<>
       (for [building buildings
             :let [[[x y] [type opts _ _ state]] building
                   vp-bx (- x vp-x)
                   vp-by (- y vp-y)]
             :when (and (>= vp-bx 0)
                        (>= vp-by 0)
                        (< vp-bx vp-w)
                        (< vp-by vp-h))]
         [:div {:key (str x y type opts)
                :class (building-tile [type opts])
                :style {:grid-column (inc vp-bx) :grid-row (inc vp-by)}}
          (when state
            (:count state))])]

      ;; resources
      [:<>
       (for [resource res
             :let [[[x y] [type opts dx dy]] resource
                   vp-rx (- x vp-x)
                   vp-ry (- y vp-y)]
             :when (and (>= vp-rx 0)
                        (>= vp-ry 0)
                        (< vp-rx vp-w)
                        (< vp-ry vp-h))]
         [:div {:key (hash (str x y type dx dy "1"))
                :class (str "char char-h")
                :style {:margin-left (str (* 2 dx) "px")
                        :margin-top  (str (* 2 dy) "px")
                        :grid-column (inc vp-rx) :grid-row (inc vp-ry)}}])]

      ;; mines
      [:<>
       (for [mine mines
             :let [[[x y] [type opts dx dy]] mine
                   vp-mx (- x vp-x)
                   vp-my (- y vp-y)]
             :when (and (>= vp-mx 0)
                        (>= vp-my 0)
                        (< vp-mx vp-w)
                        (< vp-my vp-h))]
         [:div {:key (str x y type)
                :class (str "char mine-h")
                :style {:grid-column (inc vp-mx) :grid-row (inc vp-my)}}])]]]))
