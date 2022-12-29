(ns app.pages.index.scene)

(def scene (atom {:dirty false}))

(defn init-scene [tile-width tile-height]
  (reset! scene
          {:dirty true
           :tile-shape {:width tile-width
                        :height tile-height}}))

(defn add-camera [camera-id ini-x ini-y ini-w ini-h]
  (swap! scene
         assoc-in [:camera camera-id]
         {:x ini-x
          :y ini-y
          :w ini-w
          :h ini-h
          :X (dec (+ ini-x ini-w))
          :Y (dec (+ ini-y ini-h))}))

(defn move-camera [camera-id dx dy]
  (swap! scene
         update-in [:camera camera-id]
         (fn [camera]
           {:x (+ dx (:x camera))
            :y (+ dy (:y camera))
            :w (:w camera)
            :h (:h camera)
            :X (+ dx (:X camera))
            :Y (+ dy (:Y camera))})))

(defn move-camera-absolute [camera-id x y]
  (swap! scene
         update-in [:camera camera-id]
         (fn [camera]
           {:x x
            :y y
            :w (:w camera)
            :h (:h camera)
            :X (dec (+ x (:w camera)))
            :Y (dec (+ y (:h camera)))})))

(defn align-camera-to-object-at-position [camera-id object-id camera-x camera-y]
  (swap! scene
         assoc-in [:align camera-id]
         {:object object-id
          :x camera-x
          :y camera-y}))

(defn add-layer [layer-id layer-index]
  (swap! scene
         assoc-in [:layers layer-id]
         {:z-index layer-index}))

(defn move-layer [layer-id new-layer-index]
  (swap! scene
         assoc-in [:layers layer-id :z-index] new-layer-index))
