package gg.pistol.sweeper.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import gg.pistol.sweeper.core.SweeperPoll.Mark;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;

public class PollTest {

    private Poll poll;

    private TargetImpl target1;
    private TargetImpl target2;

    @Before
    public void setUp() throws Exception {
        target1 = mockTarget();
        target2 = mockTarget();
        poll = createPoll(target1, target2);
        poll.open();
    }

    private TargetImpl mockTarget() {
        TargetImpl target = mock(TargetImpl.class);
        when(target.compareTo(any(Target.class))).thenAnswer(new Answer<Integer>() { // for the mock to work in sets
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getMock().toString().compareTo(invocation.getArguments()[0].toString());
            }
        });
        return target;
    }

    private Poll createPoll(Target... targets) {
        DuplicateGroup dups = mock(DuplicateGroup.class);
        doReturn(Lists.newArrayList(targets)).when(dups).getTargets();
        return new Poll(dups);
    }

    @Test
    public void testPoll() {
        assertEquals(2, poll.getTargets().size());

        try {
            new Poll(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testMark() {
        assertTrue(poll.getToDeleteTargets().isEmpty());
        assertTrue(poll.getRetainedTargets().isEmpty());

        poll.mark(target1, Mark.DELETE);
        assertFalse(poll.getToDeleteTargets().isEmpty());
        assertTrue(poll.getRetainedTargets().isEmpty());

        poll.mark(target1, Mark.RETAIN);
        assertTrue(poll.getToDeleteTargets().isEmpty());
        assertFalse(poll.getRetainedTargets().isEmpty());

        poll.mark(target2, Mark.DELETE);
        assertFalse(poll.getToDeleteTargets().isEmpty());
        assertFalse(poll.getRetainedTargets().isEmpty());

        poll.mark(target1, Mark.DECIDE_LATER);
        poll.mark(target2, Mark.DECIDE_LATER);
        assertTrue(poll.getToDeleteTargets().isEmpty());
        assertTrue(poll.getRetainedTargets().isEmpty());
    }

    @Test
    public void testMarkException() {
        try {
            poll.mark(null, Mark.DECIDE_LATER);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            poll.mark(target1, null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            poll.mark(mockTarget(), Mark.DECIDE_LATER);
            fail();
        } catch (IllegalStateException e) {
            // expected because the target is not from this poll
        }

        poll.close();
        try {
            poll.mark(target1, Mark.DECIDE_LATER);
            fail();
        } catch (IllegalStateException e) {
            // expected because the poll is closed
        }
    }

}
