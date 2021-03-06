(ns uncomplicate.diamond.dnn
  (:require [uncomplicate.commons.utils :refer [dragan-says-ex]]
            [uncomplicate.neanderthal
             [core :refer [ncols view transfer!]]
             [random :refer [rand-normal! rand-uniform! rng-state]]]
            [uncomplicate.diamond.tensor
             :refer [*diamond-factory* shape input output batcher]]
            [uncomplicate.diamond.internal
             [protocols :as api]
             [network :refer [sequential-network]]]))

(defprotocol Parameters
  (weights [this])
  (bias [this]))

(defn transfer-parameters! [source destination]
  (transfer! (bias source) (bias destination))
  (transfer! (weights source) (weights destination))
  destination)

(defn sum
  ([^double scale dst]
   (let [dst (api/create-tensor-desc *diamond-factory* dst)]
     (api/create-sum *diamond-factory* scale dst)))
  ([dst ^double scale src]
   (let [fact *diamond-factory*]
     (api/create-sum fact (api/create-tensor-desc fact dst)
                     scale (api/create-tensor-desc fact src) nil)))
  ([fact dst scale src & scales-srcs]
   (api/create-sum (api/factory fact) (api/create-tensor-desc fact dst)
                   scale (api/create-tensor-desc fact src)
                   (map #(if (number? %) % (api/create-tensor-desc fact %))
                        scales-srcs))))

(defn activation
  ([fact src-desc activ alpha beta]
   (api/activ-blueprint (api/factory fact) src-desc activ alpha beta))
  ([fact src-desc activ alpha]
   (api/activ-blueprint (api/factory fact) src-desc activ alpha 0.0))
  ([fact src-desc activ]
   (api/activ-blueprint (api/factory fact) src-desc activ 0.0 0.0))
  ([src-desc activ]
   (api/activ-blueprint *diamond-factory* src-desc activ 0.0 0.0)))

(defn inner-product
  ([fact src-desc dst-desc weights-type]
   (api/inner-product-blueprint (api/factory fact) src-desc dst-desc weights-type))
  ([fact src-desc dst-desc]
   (api/inner-product-blueprint (api/factory fact) src-desc dst-desc nil))
  ([src-desc dst-desc]
   (api/inner-product-blueprint *diamond-factory* src-desc dst-desc nil)))

(defn fully-connected
  ([fact src-desc dst-desc activ args]
   (let [alpha (or (:alpha args) (if (= activ :linear) 1.0 0.0))
         beta (or (:beta args) 0.0)]
     (api/fc-blueprint (api/factory fact) src-desc dst-desc
                       activ alpha beta (:weights-type args))))
  ([fact src-desc dst-desc activ]
   (api/fc-blueprint (api/factory fact) src-desc dst-desc activ
                     (if (= activ :linear) 1.0 0.0) 0.0 nil))
  ([dst-desc activ args]
   (fn
     ([fact src-desc]
      (let [dst-desc (into [(get (shape src-desc) 0)] dst-desc)]
        (fully-connected fact src-desc dst-desc activ args)))
     ([]
      dst-desc)))
  ([dst-desc activ]
   (fn
     ([fact src-desc]
      (let [dst-desc (into [(get (shape src-desc) 0)] dst-desc)]
        (fully-connected fact src-desc dst-desc activ)))
     ([]
      dst-desc))))

(defn cost
  ([layer train-tz cost]
   ((case cost
      :quadratic api/quadratic-cost
      :mean-absolute api/mean-absolute-cost
      :sigmoid-crossentropy api/sigmoid-crossentropy-cost
      (dragan-says-ex "This cost function is not supported." {:cost cost}))
    (api/factory layer) layer train-tz))
  ([layer train-tz]
   (api/quadratic-cost (api/factory layer) layer train-tz)))

(defn network
  ([fact src-desc layers]
   (sequential-network (api/factory fact) src-desc layers))
  ([src-desc layers]
   (network *diamond-factory* src-desc layers)))

(defn init! [network!]
  (doseq [layer (api/layers network!)]
    (rand-normal! 0.0 (/ 1.0 (double (apply * (rest (shape (input layer)))))) (view (weights layer)))
    (rand-normal! (view (bias layer))))
  network!)

(defn ^:private linear-decay
  [^long t ^long tau ^double eta-0 ^double eta-tau]
  (let [alpha (min (double (/ t tau)) 1.0)]
    (+  (* (- 1.0 alpha) eta-0) (* alpha eta-tau))))

(defn train
  ([network cost! epochs hyperparam]
   (let [hyperparam (transient (into [0] hyperparam))]
     (dotimes [t epochs]
       (assoc! hyperparam 0 t)
       (api/forward network hyperparam)
       (api/forward cost!)
       (api/backward cost!)
       (api/backward network hyperparam)))
   (network)
   (cost!))
  ([network cost! options]
   (map (fn [[epochs hyperparam]]
          (train network cost! epochs hyperparam))
        options))
  ([network in-batcher out-batcher cost! epochs hyperparam]
   (let [b-size (long (first (shape (input in-batcher))))
         mb-size (long (first (shape (output in-batcher))))
         mb-count (quot b-size mb-size)
         [eta-decay eta-0 eta-tau]
         (let [eta (first hyperparam)]
           (cond
             (number? eta) [linear-decay eta (* 0.01 (double eta))]
             (sequential? eta) (cons linear-decay eta)
             :default (cons (constantly nil) eta)))
         hyperparam (transient (into [0 0] (rest hyperparam)))]
     (dotimes [t (long epochs)]
       (assoc! hyperparam 0 t)
       (assoc! hyperparam 1 (eta-decay t epochs eta-0 eta-tau))
       (dotimes [n mb-count]
         (in-batcher (* n mb-size))
         (out-batcher (* n mb-size))
         (api/forward network hyperparam)
         (api/forward cost!)
         (api/backward cost!)
         (api/backward network hyperparam)))
     (network)
     (cost!)))
  ([network in-batcher out-batcher cost! options]
   (map (fn [[epochs hyperparam]]
          (train network in-batcher out-batcher cost! epochs hyperparam))
        options)))
