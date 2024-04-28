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

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {
    public interface FridgeCommand {}

    public static final class ConsumeProductCommand implements FridgeCommand {
        Product product;

        public ConsumeProductCommand(Product product) {
            this.product = product;
        }
    }

    public static final class OrderProductCommand implements FridgeCommand {
        Product product;

        public OrderProductCommand(Product product) {
            this.product = product;
        }
    }

    public static final class AddProductCommand implements FridgeCommand {
        Product product;

        public AddProductCommand(Product product) {
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
            ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor,
            ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> new Fridge(
                context,
                groupId,
                deviceId
        ));
    }

    public Fridge(
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
        if (products.contains(consumeProductCommand.product)) {
            products.remove(consumeProductCommand.product);
            getContext().getLog().info("Product {} consumed", consumeProductCommand.product.getName());
            fridgeSpaceSensor.tell(new FridgeSpaceSensor.SpaceChangedCommand(products.size()));
            fridgeWeightSensor.tell(new FridgeWeightSensor.WeightChangedCommand(getUsedWeight()));

            if (!products.contains(consumeProductCommand.product)) {
                getContext().getLog().info("Product {} out of stock, placing order.", consumeProductCommand.product.getName());
                getContext().getSelf().tell(new OrderProductCommand(consumeProductCommand.product));
            }
        } else {
            getContext().getLog().info("Product {} not found in fridge", consumeProductCommand.product.getName());
        }

        return this;
    }

    private Behavior<FridgeCommand> onOrderProduct(OrderProductCommand orderProductCommand) {
        getContext().spawn(FridgeOrderProcessor.create(getContext().getSelf(), fridgeSpaceSensor, fridgeWeightSensor, orderProductCommand.product, "1", "1"), "FridgeOrderProcessor-" + orderProductCommand.product.getName());

        return this;
    }

    private Behavior<FridgeCommand> onAddProduct(AddProductCommand addProductCommand) {
        products.add(addProductCommand.product);
        fridgeSpaceSensor.tell(new FridgeSpaceSensor.SpaceChangedCommand(products.size()));
        fridgeWeightSensor.tell(new FridgeWeightSensor.WeightChangedCommand(getUsedWeight()));
        getContext().getLog().info("Product {} added to fridge", addProductCommand.product.getName());

        Order order = new Order(addProductCommand.product);
        orders.add(order);

        System.out.println("\nReceipt:\n" +
                "Order ID \t" + order.getId() + "\n" +
                "Date \t" + order.getOrderDate() + "\n" +
                "Product \t" + addProductCommand.product.getName() + "\n" +
                "Total \t€" + addProductCommand.product.getPrice() + "\n");

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
