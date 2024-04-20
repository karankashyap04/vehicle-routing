package solver.ls.MovingStrategy;

public record Move(int prevVehicle, int prevCustomerRouteIdx, int nextVehicle, int nextCustomerRouteIdx) {
}
