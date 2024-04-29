package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

public class HomeAutomationController extends AbstractBehavior<Void>{
    private final ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<Environment.EnvironmentCommand> environment;
    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private final ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private  HomeAutomationController(ActorContext<Void> context) {
        super(context);

        this.environment = getContext().spawn(Environment.create(), "Environment");

        this.airCondition = getContext().spawn(AirCondition.create("2", "1"), "AirCondition");
        this.temperatureSensor = getContext().spawn(TemperatureSensor.create(this.airCondition, this.environment, "1", "1"), "TemperatureSensor");

        this.blinds = getContext().spawn(Blinds.create("3", "1"), "Blinds");
        this.weatherSensor = getContext().spawn(WeatherSensor.create(this.blinds, this.environment, "5", "1"), "WeatherSensor");
        this.mediaStation = getContext().spawn(MediaStation.create(this.blinds, "4", "1"), "MediaStation");

        this.fridge = getContext().spawn(Fridge.create("6", "1"), "Fridge");

        ActorRef<Void> ui = getContext().spawn(UI.create(this.temperatureSensor, this.weatherSensor, this.airCondition, this.blinds, this.mediaStation), "UI");
        getContext().getLog().info("HomeAutomation Application started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private HomeAutomationController onPostStop() {
        getContext().getLog().info("HomeAutomation Application stopped");
        return this;
    }
}
