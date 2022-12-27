(ns app.resource-generator
  (:import
   [java.util UUID]))

(def types
  [:quantity :codeable-concept])

(defn uuid [] (str (UUID/randomUUID)))

(defmulti create-observation-value (fn [t] t))

(defmethod create-observation-value :quantity
  [t]
  )

(defmethod create-observation-value :codeable-concept
  [t]
  )

(defn create-observation
  [config]
  (let [{:keys [type]} config
        observation-body
        {:resourceType "Observation"
         :id (uuid)}]))


(create-observation {:type ""})

;; resourceType: Observation
;;   id: obs5
;;   status: final
;;   code:
;;     coding:
;;       - system: "http://loinc.org"
;;         code: "Diarrhea"
;;   value:
;;     CodeableConcept:
;;       coding:
;;         - system: http://snomed.info/sct
;;           code: "255604002"
;;           display: "Mild"
;;   interpretation:
;;     - coding:
;;       - system: http://hackathon2021/ObservationInterpretation-number.com
;;         code: "3"
;;         display: "3"
