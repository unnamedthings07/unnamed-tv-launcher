package com.unnamed.tvlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class HomeEntryTimingTest {
 @Test public void remainingDelayNeverNegative(){assertEquals(600,HomeEntryTiming.remainingMillis(100,500,1000));assertEquals(0,HomeEntryTiming.remainingMillis(100,1200,1000));}
 @Test public void clockGoingBackDoesNotExtendElapsedNegative(){assertEquals(1000,HomeEntryTiming.remainingMillis(500,400,1000));}
}
