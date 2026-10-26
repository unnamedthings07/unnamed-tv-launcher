package com.unnamed.tvlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class GridNavigationTest {
 @Test public void clampKeepsIndexInRange(){assertEquals(0,GridNavigation.clampIndex(-2,4));assertEquals(3,GridNavigation.clampIndex(8,4));}
 @Test public void horizontalMovementStopsAtRowEdges(){assertEquals(0,GridNavigation.moveHorizontal(0,12,-1));assertEquals(5,GridNavigation.moveHorizontal(5,12,1));assertEquals(4,GridNavigation.moveHorizontal(5,12,-1));}
 @Test public void verticalMovementPreservesColumn(){assertEquals(8,GridNavigation.moveVertical(2,18,1,2));assertEquals(2,GridNavigation.moveVertical(8,18,-1,2));}
 @Test public void incompleteFinalRowClampsToLastItem(){assertEquals(13,GridNavigation.moveVertical(7,14,1,1));}
}
