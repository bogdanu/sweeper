package gg.pistol.sweeper.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import gg.pistol.sweeper.core.SweeperPoll.Mark;
import gg.pistol.sweeper.test.ObjectVerifier;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

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
        return target;
    }

    private Poll createPoll(TargetImpl... targets) {
        DuplicateGroup duplicateGroup = mock(DuplicateGroup.class);
        return new Poll(duplicateGroup, ImmutableSet.copyOf(targets));
    }

    @Test
    public void testPoll() {
        assertEquals(2, poll.getTargets().size());

        try {
            new Poll(poll.getDuplicateGroup(), null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testMark() {
        assertTrue(poll.getToDeleteTargets().isEmpty());
        assertTrue(poll.getRetainedTargets().isEmpty());
        assertEquals(Mark.DECIDE_LATER, poll.getMark(target1));
        assertEquals(Mark.DECIDE_LATER, poll.getMark(target2));

        poll.mark(target1, Mark.DELETE);
        assertEquals(Mark.DELETE, poll.getMark(target1));
        assertFalse(poll.getToDeleteTargets().isEmpty());
        assertTrue(poll.getRetainedTargets().isEmpty());

        poll.mark(target1, Mark.RETAIN);
        assertEquals(Mark.RETAIN, poll.getMark(target1));
        assertTrue(poll.getToDeleteTargets().isEmpty());
        assertFalse(poll.getRetainedTargets().isEmpty());

        poll.mark(target2, Mark.DELETE);
        assertEquals(Mark.DELETE, poll.getMark(target2));
        assertFalse(poll.getToDeleteTargets().isEmpty());
        assertFalse(poll.getRetainedTargets().isEmpty());

        poll.mark(target1, Mark.DECIDE_LATER);
        poll.mark(target2, Mark.DECIDE_LATER);
        assertEquals(Mark.DECIDE_LATER, poll.getMark(target1));
        assertEquals(Mark.DECIDE_LATER, poll.getMark(target2));
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

    @Test
    public void testEquals() {
        Poll pollCopy = poll.clone();
        Poll otherPoll = poll.clone();
        otherPoll.open();
        otherPoll.mark(target1, Mark.DELETE);

        ObjectVerifier.verifyEquals(poll, pollCopy, otherPoll);
    }

}
