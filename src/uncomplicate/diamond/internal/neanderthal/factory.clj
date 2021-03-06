;;   Copyright (c) Dragan Djuric. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns uncomplicate.diamond.internal.neanderthal.factory
  (:require [uncomplicate.commons
             [core :refer [Releaseable release let-release]]
             [utils :refer [dragan-says-ex]]]
            [uncomplicate.neanderthal.internal.api :refer [FlowProvider flow]]
            [uncomplicate.diamond.tensor :refer [*diamond-factory* view-tz output]]
            [uncomplicate.diamond.internal.protocols
             :refer [TensorFactory FactoryProvider ContextProvider CostFactory DnnFactory]]
            [uncomplicate.diamond.internal.dnnl
             [protocols :refer [desc]]
             [core :refer [memory-desc engine stream memory dims]]
             [tensor :refer [dnnl-tensor dnnl-transformer dnnl-batcher dnnl-shuffler]]
             [fully-connected :refer [dnnl-sum-blueprint dnnl-activ-blueprint
                                      dnnl-inner-product-blueprint dnnl-fc-blueprint
                                      dnnl-universal-cost quadratic-cost mean-absolute-cost
                                      dnnl-custom-cost sigmoid-crossentropy-cost]]]
            [uncomplicate.diamond.internal.neanderthal.fully-connected
             :refer [neanderthal-fc-blueprint]]))

(defrecord NeanderthalFactory [eng strm master]
  Releaseable
  (release [_]
    (when master
      (release strm)
      (release eng))
    true)
  FactoryProvider
  (factory [this]
    this)
  FlowProvider
  (flow [_]
    strm)
  ContextProvider
  (context [_]
    eng)
  TensorFactory
  (create-tensor-desc [this dims dtype format]
    (memory-desc dims dtype format))
  (create-tensor-desc [this tz-desc]
    (desc tz-desc))
  (create-tensor [this tensor-desc]
    (dnnl-tensor this tensor-desc))
  (create-transformer [_ in-tz out-tz]
    (dnnl-transformer eng strm (view-tz in-tz) (view-tz out-tz)))
  (create-shuffler [_ src-tz dst-tz]
    (dnnl-shuffler eng strm (view-tz src-tz) (view-tz dst-tz)))
  (create-batcher [_ src-tz dst-tz mb-size]
    (dnnl-batcher eng strm (view-tz src-tz) (view-tz dst-tz) mb-size))
  (create-sum [_ scale dst]
    (dnnl-sum-blueprint eng strm scale dst))
  (create-sum [_ dst scale src scale-srcs]
    (dnnl-sum-blueprint eng strm dst scale src scale-srcs))
  DnnFactory
  (activ-blueprint [this src-desc activ alpha beta]
    (dnnl-activ-blueprint this eng src-desc src-desc activ alpha beta))
  (inner-product-blueprint [this src-desc dst-desc weights-type]
    (dragan-says-ex "Neanderthal engine does not implement inner product blueprint."))
  (fc-blueprint [this src-desc dst-desc activ alpha beta _]
    (neanderthal-fc-blueprint this eng src-desc dst-desc activ alpha beta))
  CostFactory
  (quadratic-cost [this prev-layer train-tz]
    (dnnl-universal-cost eng strm prev-layer train-tz quadratic-cost))
  (mean-absolute-cost [this prev-layer train-tz]
    (dnnl-universal-cost eng strm prev-layer train-tz mean-absolute-cost))
  (sigmoid-crossentropy-cost [this prev-layer train-tz]
    (dnnl-custom-cost eng strm prev-layer train-tz
                        (partial sigmoid-crossentropy-cost
                                 ((dims (output prev-layer)) 0)))))

(defn neanderthal-factory
  ([eng strm]
   (->NeanderthalFactory eng strm false))
  ([]
   (let-release [eng (engine)]
     (->NeanderthalFactory eng (stream eng) true))))
