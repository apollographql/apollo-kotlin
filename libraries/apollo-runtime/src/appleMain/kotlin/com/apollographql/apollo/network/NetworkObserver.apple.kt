package com.apollographql.apollo.network

import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_monitor_update_handler_t
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_t
import platform.darwin.dispatch_queue_create

internal class AppleNetworkObserver: NetworkObserver, nw_path_monitor_update_handler_t {
  var monitor: nw_path_monitor_t = null
  var listener: NetworkObserver.Listener? = null

  override fun close() {
    if (monitor != null) {
      nw_path_monitor_cancel(monitor)
    }
  }

  override fun setListener(listener: NetworkObserver.Listener) {
    check(monitor == null) {
      "Apollo: there can be only one listener"
    }
    monitor = nw_path_monitor_create()
    this.listener = listener
    nw_path_monitor_set_update_handler(monitor, this)
    nw_path_monitor_set_queue(monitor, dispatch_queue_create("NWPath", null))
    nw_path_monitor_start(monitor)
  }

  override fun invoke(p1: nw_path_t) {
    listener?.networkChanged((nw_path_get_status(p1) == nw_path_status_satisfied))
  }
}