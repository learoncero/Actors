package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public interface EnvironmentCommand {}

    public static final class TemperatureChangeCommand implements EnvironmentCommand {
    }

    public static final class WeatherChangeCommand implements EnvironmentCommand {
    }

    public static final class SetWeatherCommand implements EnvironmentCommand {
        private final Weather weather;

        public SetWeatherCommand(Weather weather) {
            this.weather = weather;
        }
    }

    public static final class SetTemperatureCommand implements EnvironmentCommand {
        private final Temperature temperature;

        public SetTemperatureCommand(Temperature temperature) {
            this.temperature = temperature;
        }
    }

    public static final class ReceiveTemperatureRequestCommand implements EnvironmentCommand {
        ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;

        public ReceiveTemperatureRequestCommand(ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor) {
            this.temperatureSensor = temperatureSensor;
        }
    }

    public static final class ReceiveWeatherRequestCommand implements EnvironmentCommand {
        ActorRef<WeatherSensor.WeatherCommand> weatherSensor;

        public ReceiveWeatherRequestCommand(ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
            this.weatherSensor = weatherSensor;
        }
    }

    private Temperature temperature = new Temperature(21.0, "Celsius");
    private Weather weather = Weather.SUNNY;
    private final Random random = new Random();
    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;

    public static Behavior<EnvironmentCommand> create(){
        return Behaviors.setup(context ->  Behaviors.withTimers(timers -> new Environment(context, timers, timers)));
    }

    private Environment(ActorContext<EnvironmentCommand> context, TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer) {
        super(context);
        this.temperatureTimeScheduler = tempTimer;
        this.weatherTimeScheduler = weatherTimer;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChangeCommand(), Duration.ofSeconds(10));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherChangeCommand(), Duration.ofSeconds(10));
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureChangeCommand.class, this::onChangeTemperature)
                .onMessage(WeatherChangeCommand.class, this::onChangeWeather)
                .onMessage(SetWeatherCommand.class, this::setWeather)
                .onMessage(SetTemperatureCommand.class, this::setTemperature)
                .onMessage(ReceiveTemperatureRequestCommand.class, this::onReceiveTemperatureRequest)
                .onMessage(ReceiveWeatherRequestCommand.class, this::onReceiveWeatherRequest)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<EnvironmentCommand> onChangeTemperature(TemperatureChangeCommand t) {
        double rangeMin = -1.5;
        double rangeMax = 1.5;
        double change = rangeMin + (rangeMax - rangeMin) * random.nextDouble();

        this.temperature = new Temperature(temperature.getValue() + change, temperature.getUnit());

        getContext().getLog().info("Environment received {} {}", temperature.getValue(), temperature.getUnit());

        return this;
    }

    private Behavior<EnvironmentCommand> onChangeWeather(WeatherChangeCommand w) {
        if (random.nextBoolean()){
            this.weather = Weather.SUNNY;
        } else {
            this.weather = Weather.CLOUDY;
        }

        getContext().getLog().info("Weather changed to {}", weather);

        return this;
    }

    private Behavior<EnvironmentCommand> setWeather(SetWeatherCommand w) {
        getContext().getLog().info("Environment Change Weather to {}", w.weather);
        this.weather = w.weather;

        return this;
    }

    private Behavior<EnvironmentCommand> setTemperature(SetTemperatureCommand t) {
        getContext().getLog().info("Environment Change Temperature to {}", t.temperature.getValue());
        this.temperature = t.temperature;

        return this;
    }

    private Behavior<EnvironmentCommand> onReceiveTemperatureRequest(ReceiveTemperatureRequestCommand request) {
        request.temperatureSensor.tell(new TemperatureSensor.ReadTemperatureCommand(Optional.of(temperature)));
        return this;
    }

    private Behavior<EnvironmentCommand> onReceiveWeatherRequest(ReceiveWeatherRequestCommand request) {
        request.weatherSensor.tell(new WeatherSensor.ReadWeatherCommand(Optional.of(weather)));
        return this;
    }

    private Environment onPostStop(){
        getContext().getLog().info("Environment actor stopped");
        return this;
    }
}


