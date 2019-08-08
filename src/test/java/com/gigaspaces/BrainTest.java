package com.gigaspaces;

import com.gigaspaces.actions.Action;
import com.gigaspaces.actions.NotifyBeforeStopAction;
import com.gigaspaces.actions.StopAction;
import io.vavr.control.Option;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;
import software.amazon.awssdk.regions.Region;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsEmptyCollection.*;
import static org.junit.Assert.*;

public class BrainTest {

    @BeforeClass
    public static void setUp() {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    @AfterClass
    public static void tearDown() {
    }

    @Test
    public void analyze() {
        Brain brain = new Brain(new GregorianCalendar(2013,0,31), Collections.singletonList(new Suspect("denysn", "denysn@gigaspaces.com", Tz.EU)));
        Instance instance = new Instance("profile", Region.US_WEST_1, Option.some("name"), "instanceId", "foo", "Foo", Instant.now(), "type", false, "denysn");
        List<Action> actions = brain.analyze(new GregorianCalendar(1970, Calendar.JANUARY, 10), Collections.singletonList(instance));
        assertThat(actions.size(), is(1));
        assertThat(actions.get(0).getClass(), equalTo(NotifyBeforeStopAction.class));
        actions = brain.analyze(new GregorianCalendar(1970, Calendar.JANUARY, 10), Collections.singletonList(instance));
        assertThat(actions, is(empty()));
        actions = brain.analyze(new GregorianCalendar(1970, Calendar.JANUARY, 10, 2, 0, 0), Collections.singletonList(instance));
        assertThat(actions.size(), is(1));
        assertThat(actions.get(0).getClass(), equalTo(StopAction.class));
    }

}