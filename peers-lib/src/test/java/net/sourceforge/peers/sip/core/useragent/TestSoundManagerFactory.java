package net.sourceforge.peers.sip.core.useragent;

import net.sourceforge.peers.media.AbstractSoundManager;
import net.sourceforge.peers.media.AbstractSoundManagerFactory;

/**
 * @author Vitaly Alekseev
 * @since 4/29/2020
 */
public class TestSoundManagerFactory implements AbstractSoundManagerFactory {

    private final AbstractSoundManager soundManager = new DummySoundManager();

    @Override
    public AbstractSoundManager getSoundManager() {
        return soundManager;
    }
}
