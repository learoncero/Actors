package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;

import java.util.Optional;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private final ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final ActorRef<MediaStation.MediaStationCommand> mediaStation;


    public static Behavior<Void> create(
            ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor,
            ActorRef<WeatherSensor.WeatherCommand> weatherSensor,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<Blinds.BlindsCommand> blinds,
            ActorRef<MediaStation.MediaStationCommand> mediaStation
    ) {
        return Behaviors.setup(context -> new UI(context, temperatureSensor, weatherSensor, airCondition, blinds, mediaStation));
    }

    private  UI(
            ActorContext<Void> context,
            ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor,
            ActorRef<WeatherSensor.WeatherCommand> weatherSensor,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<Blinds.BlindsCommand> blinds,
            ActorRef<MediaStation.MediaStationCommand> mediaStation
    ) {
        super(context);

        this.temperatureSensor = temperatureSensor;
        this.weatherSensor = weatherSensor;
        this.airCondition = airCondition;
        this.blinds = blinds;
        this.mediaStation = mediaStation;

        new Thread(() -> { this.runCommandLine(); }).start();

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        Scanner scanner = new Scanner(System.in);
        String reader = "";


        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            // TODO: change input handling
            String[] command = reader.split(" ");
            if(command[0].equals("t")) {
                Temperature temperature = new Temperature(Double.valueOf(command[1]), String.valueOf(command[2]));
                this.temperatureSensor.tell(new TemperatureSensor.ReadTemperatureCommand(Optional.of(temperature)));
            }
            if(command[0].equals("w")) {
                Weather weather = Weather.valueOf(command[1].toUpperCase());
                this.weatherSensor.tell(new WeatherSensor.ReadWeatherCommand(Optional.of(weather)));
            }
            if(command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirConditionCommand(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("m")) {
                if(command[1].equals("play")) {
                    this.mediaStation.tell(new MediaStation.PlayMediaCommand());
                } else if(command[1].equals("stop")) {
                    this.mediaStation.tell(new MediaStation.StopMediaCommand());
                }
            }

            if(command[0].equals("stop")) {
                getContext().getSystem().terminate();
            }

            if(command[0].equals("help")) {
                System.out.println("Commands:");
                System.out.println("t [temperature] [unit]              - Set temperature");
                System.out.println("w [sunny|cloudy]                    - Set weather");
                System.out.println("a [true|false]                      - Power on/off air condition");
                System.out.println("m [play|stop]                       - Play/stop media");
                System.out.println("stop                                - Stop application");
            }
        }
        getContext().getLog().info("UI done");
    }
}
