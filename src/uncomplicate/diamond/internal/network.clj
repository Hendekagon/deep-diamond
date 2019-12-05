(ns uncomplicate.diamond.internal.network
  (:require [uncomplicate.commons.core :refer [Releaseable release let-release]]
            [uncomplicate.diamond.tensor :refer [Transfer input output]]
            [uncomplicate.diamond.internal.protocols
             :refer [NeuralNetwork Backprop forward backward FactoryProvider factory]])
  (:import clojure.lang.IFn))

(defn invoke [f]
  (f))

(deftype SequentialNetworkInference [forward-layers]
  Releaseable
  (release [_]
    (peek (mapv release forward-layers)))
  FactoryProvider
  (factory [_]
    (factory (peek forward-layers)))
  NeuralNetwork
  (layers [_]
    forward-layers)
  Transfer
  (input [_] (input (get forward-layers 0)))
  (output [_] (output (peek forward-layers)))
  IFn
  (invoke [this]
    (peek (mapv invoke forward-layers))))

(deftype SequentialNetworkTraining [forward-layers last-layer rest-backward-layers]
  Releaseable
  (release [_]
    (doseq [l forward-layers] (release l)))
  FactoryProvider
  (factory [_]
    (factory last-layer))
  NeuralNetwork
  (layers [_]
    forward-layers)
  Transfer
  (input [_] (input (first forward-layers)))
  (output [_] (output last-layer))
  IFn
  (invoke [this]
    (doseq [layer forward-layers]
      (layer))
    (output last-layer))
  Backprop
  (forward [this hyperparam]
    (doseq [layer forward-layers]
      (forward layer hyperparam))
    this)
  (backward [this]
    (backward last-layer))
  (backward [this hyperparam]
    (backward last-layer hyperparam)
    (doseq [layer rest-backward-layers]
      (backward layer)
      (backward layer hyperparam))
    this))

(deftype SequentialNetworkBlueprint [layer-blueprints]
  Releaseable
  (release [_]
    (doseq [l layer-blueprints] (release l)))
  NeuralNetwork
  (layers [_]
    layer-blueprints)
  IFn
  (invoke [_ input-tz optimization]
    (loop [bps (rest layer-blueprints)
           backward-layers [((first layer-blueprints) input-tz false optimization)]]
      (if (first bps)
        (recur (rest bps)
               (cons ((first bps) (first backward-layers) true optimization)
                     backward-layers))
        (->SequentialNetworkTraining (reverse backward-layers)
                                     (first backward-layers)
                                     (rest backward-layers)))))
  (invoke [this input-tz]
    (loop [bps (rest layer-blueprints)
           forward-layers [((first layer-blueprints) input-tz)]]
      (if (first bps)
        (recur (rest bps) (conj forward-layers ((first bps) (peek forward-layers))))
        (->SequentialNetworkInference forward-layers)))))

(defn sequential-network [fact src-desc layers]
  (let-release [layers (reduce (fn [lrs layer-fn]
                                 (conj lrs (layer-fn fact (peek lrs))))
                               [((first layers) fact src-desc)]
                               (rest layers))]
    (->SequentialNetworkBlueprint layers)))