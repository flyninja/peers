package net.sourceforge.peers.media;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;

public class ConfigAbstractSoundManagerFactory implements AbstractSoundManagerFactory {

    private Config config;
    private String peersHome;
    private Logger logger;

    public ConfigAbstractSoundManagerFactory(Config config, String peersHome, Logger logger) {
        this.config = config;
        this.peersHome = peersHome;
        this.logger = logger;
    }

    @Override
    public AbstractSoundManager getSoundManager()  {
        switch (config.getMediaMode()) {
            // TODO: valekseev: 4/29/2020: only file mode supporting in this implementation
            case file:
                return new FilePlaybackSoundManager(config.getMediaFile(), config.getMediaFileDataFormat(), logger);
            default:
                return null;
        }
    }

}
