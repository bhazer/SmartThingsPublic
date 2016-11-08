definition(
    name: "Number One Fan",
    namespace: "bhazer",
    author: "Bart Hazer",
    description: "Fan control",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Switch") {
		input "theswitch", "capability.switch", required: true
	}
	section("Times") {
		input "time1", "number", title: "How long to stay on when switched on?", required: true
		input "time2", "number", title: "How long to stay on when double tapped?", required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(theswitch, "switch.on", switchedOn)
	subscribe(theswitch, "switch.off", switchedOff)
	subscribe(theswitch, "button.pushed", pushed)
}

// TODO: implement event handlers
