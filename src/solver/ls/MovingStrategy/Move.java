package solver.ls.MovingStrategy;

import java.util.List;

public record Move(List<Integer> prevVehicle, List<Integer> prevCustomerRouteIdx, List<Integer> nextVehicle, List<Integer> nextCustomerRouteIdx) {
}
