package com.williamhill.permission.domain

case class ActionsPerUniverse(universe: String, event: String, status: String, actions: List[String])

object ActionsPerUniverse {

  def find(universe: String, event: String, status: String): List[Action] =
    loadFromConfig
      .filter(x => x.universe == universe && x.event == event && x.status == status)
      .flatMap(_.actions)
      .flatMap(actionName => Action.fromConfig.find(_.name == actionName))

  def loadFromConfig: List[ActionsPerUniverse] = ??? //TODO load from config
}
