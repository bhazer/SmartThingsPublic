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
    section("Devices") {
        input "theswitch", "capability.switch", title: "Which fan switch?", required: true
        input "sensor", "capability.relativeHumidityMeasurement", title: "Which humidity measurement?", required: true
    }
    section("Timer") {
        input "time1", "number", title: "How long to stay on when switched on?", required: true
        input "time2", "number", title: "How long to stay on when double tapped?", required: true
        input "time3", "number", title: "How long to stay on when triple tapped?", required: true
    }
    section("Humidity Control") {
        input "dewPointTurnOn", "number", title: "Turn on when dew point goes above? (in F)", required: true
        input "dewPointOnUntil", "number", title: "Run until dew point goes back to? (in F)", required: true
        input "maxHumidityTime", "number", title: "Max time to run to control humidity", required: true
    }
}

mappings {
    path("/humidity/up") {
        action: [
            POST: "humidityUp"
        ]
    }
    path("/humidity/down") {
        action: [
            POST: "humidityDown"
        ]
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
    state.mode = (theswitch.currentSwitch == "on") ? "manual" : "off"
    state.offAt = -1

    subscribe(theswitch, "switch.on", switchedOn)
    subscribe(theswitch, "switch.off", switchedOff)
    subscribe(theswitch, "button.pushed", pushed)
    subscribe(sensor, "humidity", sensorChanged)
    subscribe(sensor, "temperature", sensorChanged)

    checkDewPoint()
}

def switchedOn(evt) {
    if (state.mode == "off") {
        startTimer(time1)
    }
}

def switchedOff(evt) {
    log.debug "Switched off manually"
    state.mode = "off"
    state.offAt = -1
}

def pushed(evt) {
	def jsonSlurper = new groovy.json.JsonSlurper()
    def data = jsonSlurper.parseText(evt.data)
    log.debug "Button pushed: ${data}"
    switch (data.buttonNumber) {
        // double tap up
        case "1":
            theswitch.on()
            startTimer(time2)
            break

        // triple tap up
        case "3":
            theswitch.on()
            startTimer(time3)
            break

        // hold up
        case "5":
    		log.debug "Putting into manual mode"
            theswitch.on()
            state.mode = "manual"
            state.offAt = -1
            break

        // no big thang, some other unsupported gesture
        default:
            break
    }
}

def startTimer(time) {
    log.debug "Running for ${time} minutes"
    state.mode = "timed"
    state.offAt = now() + (60 * 1000 * time)
    runIn(60 * time, timerElapsed)
}

def timerElapsed() {
    log.debug "timerElapsed - mode: ${state.mode}, offAt: ${state.offAt}, now: ${now()}"
    if ((state.mode != "manual") && ((state.offAt - now()) < 10*1000)) {
	    log.debug "turning off"
        state.mode = (state.mode == "humidity" ? "humidityOverrun" : "off")
        log.debug "new mode is ${state.mode}"
        theswitch.off()
        state.offAt = -1
    }
}

def sensorChanged(evt) {
    log.debug "sensorChanged"
    checkDewPoint()
}

def checkDewPoint() {
    log.debug "currentHumidity: ${sensor.currentHumidity}"
    log.debug "currentTemperature: ${sensor.currentTemperature}"
    def dewPoint = sensor.currentTemperature - 0.36*(100-sensor.currentHumidity)
    log.debug "current dewPoint: ${dewPoint}"

    if (dewPoint >= dewPointTurnOn) {
        humidityUp()
    }
    else if (dewPoint <= dewPointOnUntil) {
        humidityDown()
    }
}

def humidityUp() {
    log.debug "elevated humidity, current mode: ${state.mode}"
    if (state.mode == "timer" || state.mode == "off") {
        log.debug "switching into humidity mode"
        theswitch.on()
        startTimer(maxHumidityTime)
        state.mode = "humidity"
    }
    return [success: "true"]
}

def humidityDown() {
    log.debug "humidity back to normal, current mode: ${state.mode}"
    if (state.mode == "humidity") {
        log.debug "turning off"
        state.mode = "off"
        theswitch.off()
        state.offAt = -1
    }
    return [success: "true"]
}
