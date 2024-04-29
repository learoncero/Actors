package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;

import java.util.Optional;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand {}

    public static final class RequestWeatherFromEnvironmentCommand implements WeatherSensor.WeatherCommand {
    }

    public static final class ReadWeatherCommand implements WeatherSensor.WeatherCommand {
        private final Optional<Weather> weather;

        public ReadWeatherCommand(Optional<Weather> weather) {
            this.weather = weather;
        }
    }

    private final String groupId;
    private final String deviceId;
    private ActorRef<Blinds.BlindsCommand> Blinds;
    private ActorRef<Environment.EnvironmentCommand> environment;

    public static Behavior<WeatherSensor.WeatherCommand> create(
            ActorRef<Blinds.BlindsCommand> Blinds,
            ActorRef<Environment.EnvironmentCommand> environment,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> Behaviors.withTimers(timer -> new WeatherSensor(context, Blinds, environment, groupId, deviceId, timer)));
    }

    private WeatherSensor(
            ActorContext<WeatherSensor.WeatherCommand> context,
            ActorRef<Blinds.BlindsCommand> Blinds,
            ActorRef<Environment.EnvironmentCommand> environment,
            String groupId,
            String deviceId,
            TimerScheduler<WeatherSensor.WeatherCommand> weatherTimeScheduler
    ) {
        super(context);
        this.Blinds = Blinds;
        this.environment = environment;
        this.groupId = groupId;
        this.deviceId = deviceId;
        weatherTimeScheduler.startTimerAtFixedRate(new RequestWeatherFromEnvironmentCommand(), java.time.Duration.ofSeconds(10));

        getContext().getLog().info("WeatherSensor started");
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadWeatherCommand.class, this::onReadWeather)
                .onMessage(RequestWeatherFromEnvironmentCommand.class, this::onRequestWeatherFromEnvironment)
                .build();
    }

    private Behavior<WeatherCommand> onRequestWeatherFromEnvironment(RequestWeatherFromEnvironmentCommand requestWeatherFromEnvironmentCommand) {
        this.environment.tell(new Environment.ReceiveWeatherRequestCommand(getContext().getSelf()));
        return this;
    }

    private Behavior<WeatherCommand> onReadWeather(ReadWeatherCommand readWeatherCommand) {
        if (!readWeatherCommand.weather.isPresent()) {
            getContext().getLog().info("WeatherSensor received no weather");
            return this;
        }
        Weather weather = readWeatherCommand.weather.get();
        getContext().getLog().info("WeatherSensor received {}", weather);

        if (weather == Weather.SUNNY) {
            this.Blinds.tell(new Blinds.CloseBlindsCommand());
        } else {
            this.Blinds.tell(new Blinds.OpenBlindsCommand());
        }

        return this;
    }
}
