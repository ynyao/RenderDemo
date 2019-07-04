package com.zcy.renderdemo.render

import com.zcy.renderdemo.mediator.Mediator

abstract class Colleague(mediator:Mediator) {
    var mediator: Mediator? = null
    init {

    }

}