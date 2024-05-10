package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.Order;
import at.fhv.sysarch.lab2.homeautomation.domain.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {
    public interface FridgeCommand {}

    public static final class ConsumeProductCommand implements FridgeCommand {
        private final Optional<Product> product;

        public ConsumeProductCommand(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class OrderProductCommand implements FridgeCommand {
        private final Optional<Product> product;

        public OrderProductCommand(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class AddProductCommand implements FridgeCommand {
        private final Optional<Product> product;

        public AddProductCommand(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class OrderHistoryCommand implements FridgeCommand {
    }

    public static final class QueryStockCommand implements FridgeCommand {
    }

    private final String groupId;
    private final String deviceId;
    private final List<Product> products;
    private final List<Order> orders;
    private final ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor;
    private final ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor;

    public static Behavior<Fridge.FridgeCommand> create(
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> new Fridge(
                context,
                groupId,
                deviceId
        ));
    }

    private Fridge(
            ActorContext<FridgeCommand> context,
            String groupId,
            String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        this.orders = new ArrayList<>();
        this.products = new ArrayList<>();
        this.products.add(new Product("Milk", 1.70, 2.1));
        this.products.add(new Product("Milk", 1.70, 2.1));
        this.products.add(new Product("Milk", 1.70, 2.1));
        this.products.add(new Product("Eggs", 3.50, 0.5));
        this.products.add(new Product("Eggs", 3.50, 0.5));
        this.products.add(new Product("Beer", 18.50, 20));
        this.products.add(new Product("Beer", 18.50, 20));

        this.fridgeSpaceSensor = getContext().spawn(FridgeSpaceSensor.create(products.size(), groupId, deviceId), "FridgeSpaceSensor");
        this.fridgeWeightSensor = getContext().spawn(FridgeWeightSensor.create(getUsedWeight(), groupId, deviceId), "FridgeWeightSensor");

        getContext().getLog().info("Fridge started");
    }

    private int getUsedWeight() {
        int weightSum = 0;
        for (Product product : products) {
            weightSum += product.getWeight();
        }

        return weightSum;
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ConsumeProductCommand.class, this::onConsumeProduct)
                .onMessage(OrderProductCommand.class, this::onOrderProduct)
                .onMessage(AddProductCommand.class, this::onAddProduct)
                .onMessage(OrderHistoryCommand.class, this::onOrderHistory)
                .onMessage(QueryStockCommand.class, this::onQueryStock)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onConsumeProduct(ConsumeProductCommand consumeProductCommand) {
        Product product;

        if (consumeProductCommand.product.isPresent()) {
            product = consumeProductCommand.product.get();
        } else {
            getContext().getLog().info("Product not found in consume command");
            return this;
        }

        if (products.contains(product)) {
            products.remove(product);
            getContext().getLog().info("Product {} consumed", product.getName());
            fridgeSpaceSensor.tell(new FridgeSpaceSensor.SpaceChangedCommand(products.size()));
            fridgeWeightSensor.tell(new FridgeWeightSensor.WeightChangedCommand(getUsedWeight()));

            if (!products.contains(product)) {
                getContext().getLog().info("Product {} out of stock, placing order.", product.getName());
                getContext().getSelf().tell(new OrderProductCommand(Optional.of(product)));
            }
        } else {
            getContext().getLog().info("Product {} not found in fridge", product.getName());
        }

        return this;
    }

    private Behavior<FridgeCommand> onOrderProduct(OrderProductCommand orderProductCommand) {
        Product product;

        if (orderProductCommand.product.isPresent()) {
            product = orderProductCommand.product.get();
        } else {
            getContext().getLog().info("Product not found in order command");
            return this;
        }

        getContext().spawn(FridgeOrderProcessor.create(getContext().getSelf(), fridgeSpaceSensor, fridgeWeightSensor,
                product, "1", "1"), "FridgeOrderProcessor-" + product.getName());

        return this;
    }

    private Behavior<FridgeCommand> onAddProduct(AddProductCommand addProductCommand) {
        Product product;

        if (addProductCommand.product.isPresent()) {
            product = addProductCommand.product.get();
        } else {
            getContext().getLog().info("Product not found in add product command");
            return this;
        }

        products.add(product);
        fridgeSpaceSensor.tell(new FridgeSpaceSensor.SpaceChangedCommand(products.size()));
        fridgeWeightSensor.tell(new FridgeWeightSensor.WeightChangedCommand(getUsedWeight()));
        getContext().getLog().info("Product {} added to fridge", product.getName());

        Order order = new Order(product);
        orders.add(order);

        System.out.println("\nReceipt:\n" +
                "Order ID \t" + order.getId() + "\n" +
                "Date \t" + order.getOrderDate() + "\n" +
                "Product \t" + product.getName() + "\n" +
                "Total \t€" + product.getPrice() + "\n");
        return this;
    }

    private Behavior<FridgeCommand> onOrderHistory(OrderHistoryCommand orderHistoryCommand) {
        System.out.println("Order history:");
        for (Order order : orders) {
            System.out.println(order);
        }
        System.out.println();

        return this;
    }

    private Behavior<FridgeCommand> onQueryStock(QueryStockCommand queryStockCommand) {
        System.out.println("Stock:");
        for (Product product : products) {
            System.out.println(product);
        }
        System.out.println();

        return this;
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor {} {} stopped", groupId, deviceId);
        return this;
    }
}
