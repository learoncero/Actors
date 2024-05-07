package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.domain.Product;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;

import java.util.Optional;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private final ActorRef<Environment.EnvironmentCommand> environment;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final ActorRef<Fridge.FridgeCommand> fridge;
    private final ActorRef<MediaStation.MediaStationCommand> mediaStation;
    //private final ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;


    public static Behavior<Void> create(

            ActorRef<Environment.EnvironmentCommand> environment,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<Blinds.BlindsCommand> blinds,
            ActorRef<Fridge.FridgeCommand> fridge,
            ActorRef<MediaStation.MediaStationCommand> mediaStation,
            //ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor,
            ActorRef<WeatherSensor.WeatherCommand> weatherSensor
    ) {
        return Behaviors.setup(context -> new UI(context, environment, airCondition, blinds, fridge, mediaStation, weatherSensor));
    }

    private  UI(
            ActorContext<Void> context,
            ActorRef<Environment.EnvironmentCommand> environment,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<Blinds.BlindsCommand> blinds,
            ActorRef<Fridge.FridgeCommand> fridge,
            ActorRef<MediaStation.MediaStationCommand> mediaStation,
            //ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor,
            ActorRef<WeatherSensor.WeatherCommand> weatherSensor
    ) {
        super(context);

        this.environment = environment;
        this.airCondition = airCondition;
        this.blinds = blinds;
        this.fridge = fridge;
        this.mediaStation = mediaStation;
        //this.temperatureSensor = temperatureSensor;
        this.weatherSensor = weatherSensor;

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
            String[] command = reader.split(" ");
            switch(command[0]) {
                case "et": {
                    Temperature temperature = new Temperature(Double.parseDouble(command[1]), "Celsius");
                    this.environment.tell(new Environment.SetTemperatureCommand(temperature));
                    break;
                }
                case "ew": {
                    Weather weather = Weather.valueOf(command[1].toUpperCase());
                    this.environment.tell(new Environment.SetWeatherCommand(weather));
                    break;
                }
                case "a": {
                    this.airCondition.tell(new AirCondition.PowerAirConditionCommand(Optional.of(Boolean.valueOf(command[1]))));
                    break;
                }
                case "m": {
                    if(command[1].equals("play")) {
                        this.mediaStation.tell(new MediaStation.PlayMediaCommand());
                    } else if(command[1].equals("stop")) {
                        this.mediaStation.tell(new MediaStation.StopMediaCommand());
                    }
                    break;
                }
                case "fc": {
                    if (command.length < 2) {
                        System.out.println("Please specify a product (milk, eggs, beer) and reenter the whole command.");
                    } else {
                        Product product;
                        switch (command[1]) {
                            case "milk":
                                product = new Product("Milk", 1.70, 2.1);
                                this.fridge.tell(new Fridge.ConsumeProductCommand(Optional.of(product)));
                                break;
                            case "eggs":
                                product = new Product("Eggs", 3.50, 0.5);
                                this.fridge.tell(new Fridge.ConsumeProductCommand(Optional.of(product)));
                                break;
                            case "beer":
                                product = new Product("Beer", 18.50, 20);
                                this.fridge.tell(new Fridge.ConsumeProductCommand(Optional.of(product)));
                                break;
                            default:
                                System.out.println("Invalid Product. Type 'help' if you want to see valid input options.");
                                return;
                        }
                    }
                }
                case "fo": {
                    if (command.length < 2) {
                        System.out.println("Please specify a product (milk, eggs, beer) and reenter the whole command.");
                    } else {
                        Product product;
                        switch (command[1]) {
                            case "milk":
                                product = new Product("Milk", 1.70, 2.1);
                                this.fridge.tell(new Fridge.OrderProductCommand(Optional.of(product)));
                                break;
                            case "eggs":
                                product = new Product("Eggs", 3.50, 0.5);
                                this.fridge.tell(new Fridge.OrderProductCommand(Optional.of(product)));
                                break;
                            case "beer":
                                product = new Product("Beer", 18.50, 20);
                                this.fridge.tell(new Fridge.OrderProductCommand(Optional.of(product)));
                                break;
                            default:
                                System.out.println("Invalid Product. Type 'help' if you want to see valid input options.");
                                break;
                        }
                    }
                    break;
                }
                case "foh": {
                    this.fridge.tell(new Fridge.OrderHistoryCommand());
                    break;
                }
                case "fs": {
                    this.fridge.tell(new Fridge.QueryStockCommand());
                    break;
                }
                case "stop": {
                    getContext().getSystem().terminate();
                    break;
                }
                case "help": {
                    System.out.println("Commands:");
                    System.out.println("et [temperature]              - Set temperature in Celsius");
                    System.out.println("ew [sunny|cloudy]             - Set weather");
                    System.out.println("fc [milk|eggs|beer]           - Consume product of choice");
                    System.out.println("fo [milk|eggs|beer]           - Order product of choice");
                    System.out.println("foh                           - Display order history");
                    System.out.println("fs                            - Display products in fridge");
                    System.out.println("a [true|false]                - Power on/off air condition");
                    System.out.println("m [play|stop]                 - Play/stop media");
                    System.out.println("stop                          - Stop application");
                    break;
                }
                default: {
                    System.out.println("Unknown command. Please enter 'help' for valid commands.");
                    break;
                }
            }
        }
        getContext().getLog().info("UI done");
    }
}
