package net.orfjackal.dimdwarf.domain

trait Timestamp extends Comparable[Timestamp] {
  def next: Timestamp
}

// TODO: timestamps based on "Time, Clocks, and the Ordering of Events
// in a Distributed System" (Lamport 1978), maybe name the class ClusterTimestamp
