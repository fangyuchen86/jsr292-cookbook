package jsr292.cookbook.mdispatch;

class AbstractPositionInfo {
  final int projectionIndex;
  final boolean mayBoxUnbox;
  
  AbstractPositionInfo(int projectionIndex, boolean mayBoxUnbox) {
    this.projectionIndex = projectionIndex;
    this.mayBoxUnbox = mayBoxUnbox;
  }
}