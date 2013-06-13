#!/usr/bin/env groovy

/**
 * Splits iso_3166_2.json into one global file + one file per country.
 */

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

def json = new File("iso_3166_2.json").text
def slurper = new JsonSlurper()
def world = slurper.parseText(json)

def locations = [:]
def top = [:]
def sorted = [:]

def get_entry = { it ->
  return [
    name: it.name,
    code: it.code,
    parent: it.parent
  ]
}

for (entry in world) {
    // divisions
    def country = entry.key[0..1]
    def the_entry = get_entry(entry.value)
    def key = null
    if (the_entry.parent) {
      // subdivision
      key = the_entry.parent
    }
    else {
      // country
      key = country
    }
    if (!locations[key]) {
      locations[key] = [:]
    }
    locations[key][entry.key] = the_entry
}

for (country in locations) {

  // Alpha sorting
  def comparator = [ compare:
    { a, b -> return a.value.name.compareTo(b.value.name) }
  ] as Comparator

  def sortedSet = new TreeSet( comparator )

  sortedSet.addAll( locations[country.key].entrySet() );

  for (entry in sortedSet) {
    sorted.put(entry.value.code, entry.value)
  }

}

def keep_going = true

while (keep_going) {
  def to_remove = []
  for (entry in sorted) {
    if (entry.value.code.size() == 2) {
      entry.value.remove("parent")
    }
    else if (entry.value.parent) {
      def children = entry.value
      if (sorted[entry.value.parent]) {
        if (!sorted[entry.value.parent].children) {
          sorted[entry.value.parent].children = [:]
        }
        def new_entry = [:]
        new_entry.putAll(entry.value)
        new_entry.remove("parent")
        sorted[entry.value.parent].children.put( new_entry.code, new_entry )
      }
      else {
      }
      to_remove.add(entry.value.code)
    }
  }

  for (key in to_remove) {
    sorted.remove(key)
  }

  if (to_remove.size() == 0) {
    keep_going = false
  }
}


for (country in sorted) {
  println country.key

  def builder = new JsonBuilder()
  builder.call(country.value)
  def file = new File("output/" + country.key + ".json").withWriter{ 
    // builder.writeTo( it )
    it << builder.toPrettyString()
    it.flush()
  }
}
