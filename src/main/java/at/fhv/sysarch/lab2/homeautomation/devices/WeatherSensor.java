package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;

import java.util.Optional;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand {}

    public static final class ReadWeather implements WeatherSensor.WeatherCommand {
        private final Optional<Weather> weather;

        public ReadWeather(Optional<Weather> weather) {
            this.weather = weather;
        }
    }

    public static Behavior<WeatherSensor.WeatherCommand> create(ActorRef<Blind.BlindCommand> blind, String groupId, String deviceId) {
        return Behaviors.setup(context -> new WeatherSensor(context, blind, groupId, deviceId));
    }

    private final String groupId;
    private final String deviceId;
    private ActorRef<Blind.BlindCommand> blind;

    public WeatherSensor(ActorContext<WeatherSensor.WeatherCommand> context, ActorRef<Blind.BlindCommand> blind, String groupId, String deviceId) {
        super(context);
        this.blind = blind;
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("WeatherSensor started");
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return null;
    }
}
