/*
 * Copyright (C) 2005-2017 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */

package net.truelicense.core;

import global.namespace.fun.io.api.*;
import net.truelicense.api.*;
import net.truelicense.api.auth.*;
import net.truelicense.api.codec.Codec;
import net.truelicense.api.comp.CompressionProvider;
import net.truelicense.api.crypto.EncryptionFactory;
import net.truelicense.api.crypto.EncryptionParameters;
import net.truelicense.api.misc.Builder;
import net.truelicense.api.misc.CachePeriodProvider;
import net.truelicense.api.misc.Clock;
import net.truelicense.api.passwd.*;
import net.truelicense.core.auth.Notary;
import net.truelicense.core.misc.Strings;
import net.truelicense.core.passwd.MinimumPasswordPolicy;
import net.truelicense.obfuscate.Obfuscate;

import javax.security.auth.x500.X500Principal;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Callable;

import static global.namespace.fun.io.bios.BIOS.*;
import static java.util.Calendar.DATE;
import static java.util.Calendar.getInstance;
import static java.util.Objects.requireNonNull;
import static net.truelicense.core.Messages.*;

/**
 * A basic license application context.
 * This class is immutable.
 * <p>
 * Unless stated otherwise, all no-argument methods need to return consistent
 * objects so that caching them is not required.
 * A returned object is considered to be consistent if it compares
 * {@linkplain Object#equals(Object) equal} or at least behaves identical to
 * any previously returned object.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings({"ConstantConditions", "OptionalUsedAsFieldOrParameterType", "unchecked", "unused", "WeakerAccess"})
public abstract class TrueLicenseApplicationContext implements LicenseApplicationContext {

    @Override
    public LicenseManagementContextBuilder context() { return new TrueLicenseManagementContextBuilder(); }

    //
    // Utility functions:
    //

    private static <V> V checked(final Callable<V> task) throws LicenseManagementException {
        try {
            return task.call();
        } catch (RuntimeException | LicenseManagementException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseManagementException(e);
        }
    }

    private static <V> V unchecked(final Callable<V> task) {
        try {
            return task.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new UncheckedLicenseManagementException(e);
        }
    }

    //
    // Inner classes:
    //

    final class TrueLicenseManagementContextBuilder implements LicenseManagementContextBuilder {

        AuthenticationFactory authenticationFactory = Notary::new;
        LicenseManagementAuthorization authorization = new TrueLicenseManagementAuthorization();
        long cachePeriodMillis = 30 * 60 * 1000;
        Clock clock = Date::new;
        Optional<Codec> codec = Optional.empty();
        Optional<Transformation> compression = Optional.empty();
        String encryptionAlgorithm = "";
        Optional<EncryptionFactory> encryptionFactory = Optional.empty();
        Optional<LicenseFactory> factory = Optional.empty();
        Optional<LicenseInitialization> initialization = Optional.empty();
        LicenseFunctionComposition initializationComposition = LicenseFunctionComposition.decorate;
        PasswordPolicy passwordPolicy = new MinimumPasswordPolicy();
        Optional<RepositoryContext<?>> repositoryContext = Optional.empty();
        String subject = "";
        String keystoreType = "";
        Optional<LicenseValidation> validation = Optional.empty();
        LicenseFunctionComposition validationComposition = LicenseFunctionComposition.decorate;

        @Override
        public LicenseManagementContextBuilder authenticationFactory(final AuthenticationFactory authenticationFactory) {
            this.authenticationFactory = requireNonNull(authenticationFactory);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder authorization(final LicenseManagementAuthorization authorization) {
            this.authorization = requireNonNull(authorization);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder cachePeriodMillis(final long cachePeriodMillis) {
            if (cachePeriodMillis < 0) {
                throw new IllegalArgumentException("" + cachePeriodMillis);
            }
            this.cachePeriodMillis = cachePeriodMillis;
            return this;
        }

        @Override
        public LicenseManagementContextBuilder clock(final Clock clock) {
            this.clock = requireNonNull(clock);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder codec(final Codec codec) {
            this.codec = Optional.of(codec);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder compression(final Transformation compression) {
            this.compression = Optional.of(compression);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder encryptionAlgorithm(final String encryptionAlgorithm) {
            this.encryptionAlgorithm = Strings.requireNonEmpty(encryptionAlgorithm);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder encryptionFactory(final EncryptionFactory encryptionFactory) {
            this.encryptionFactory = Optional.of(encryptionFactory);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder initialization(final LicenseInitialization initialization) {
            this.initialization = Optional.ofNullable(initialization);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder initializationComposition(final LicenseFunctionComposition composition) {
            this.initializationComposition = requireNonNull(composition);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder licenseFactory(final LicenseFactory factory) {
            this.factory = Optional.of(factory);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder passwordPolicy(final PasswordPolicy passwordPolicy) {
            this.passwordPolicy = requireNonNull(passwordPolicy);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder repositoryContext(final RepositoryContext<?> repositoryContext) {
            this.repositoryContext = Optional.of(repositoryContext);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder keystoreType(final String keystoreType) {
            this.keystoreType = Strings.requireNonEmpty(keystoreType);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder subject(final String subject) {
            this.subject = Strings.requireNonEmpty(subject);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder validation(final LicenseValidation validation) {
            this.validation = Optional.ofNullable(validation);
            return this;
        }

        @Override
        public LicenseManagementContextBuilder validationComposition(final LicenseFunctionComposition composition) {
            this.validationComposition = requireNonNull(composition);
            return this;
        }

        @Override
        public LicenseManagementContext build() { return new TrueLicenseManagementContext(this); }
    }

    final class TrueLicenseManagementContext
    implements
            CachePeriodProvider,
            Clock,
            CompressionProvider,
            LicenseManagementAuthorizationProvider,
            LicenseInitializationProvider,
            LicenseManagementContext,
            LicenseManagementSubjectProvider,
            LicenseValidationProvider,
            PasswordPolicyProvider,
            RepositoryContextProvider {

        final AuthenticationFactory authenticationFactory;
        final LicenseManagementAuthorization authorization;
        final long cachePeriodMillis;
        final Clock clock;
        final Codec codec;
        final Transformation compression;
        final String encryptionAlgorithm;
        final EncryptionFactory encryptionFactory;
        final LicenseFactory factory;
        final Optional<LicenseInitialization> initialization;
        final LicenseFunctionComposition initializationComposition;
        final PasswordPolicy passwordPolicy;
        final RepositoryContext<?> repositoryContext;
        final String keystoreType;
        final String subject;
        final Optional<LicenseValidation> validation;
        final LicenseFunctionComposition validationComposition;

        TrueLicenseManagementContext(final TrueLicenseManagementContextBuilder b) {
            this.authenticationFactory = b.authenticationFactory;
            this.authorization = b.authorization;
            this.cachePeriodMillis = b.cachePeriodMillis;
            this.clock = b.clock;
            this.codec = b.codec.get();
            this.compression = b .compression.get();
            this.encryptionAlgorithm = Strings.requireNonEmpty(b.encryptionAlgorithm);
            this.encryptionFactory = b.encryptionFactory.get();
            this.factory = b.factory.get();
            this.initialization = b.initialization;
            this.initializationComposition = b.initializationComposition;
            this.passwordPolicy = b.passwordPolicy;
            this.repositoryContext = b.repositoryContext.get();
            this.keystoreType = Strings.requireNonEmpty(b.keystoreType);
            this.subject = Strings.requireNonEmpty(b.subject);
            this.validation = b.validation;
            this.validationComposition = b.validationComposition;
        }

        AuthenticationFactory authenticationFactory() { return authenticationFactory; }

        @Override
        public LicenseManagementAuthorization authorization() { return authorization; }

        @Override
        public long cachePeriodMillis() { return cachePeriodMillis; }

        @Override
        public Codec codec() { return codec; }

        @Override
        public Transformation compression() { return compression; }

        @Override
        public ConsumerLicenseManagerBuilder consumer() { return new ConsumerTrueLicenseManagerBuilder(); }

        String encryptionAlgorithm() { return encryptionAlgorithm; }

        EncryptionFactory encryptionFactory() { return encryptionFactory; }

        @Override
        public LicenseInitialization initialization() {
            final LicenseInitialization second = new TrueLicenseInitialization();
            return initialization
                    .map(first -> initializationComposition.compose(first, second))
                    .orElse(second);
        }

        @Override
        public License license() { return factory.license(); }

        @Override
        public Date now() { return clock.now(); }

        @Override
        public PasswordPolicy passwordPolicy() { return passwordPolicy; }

        @Override
        public <Model> RepositoryContext<Model> repositoryContext() {
            return (RepositoryContext<Model>) repositoryContext;
        }

        String keystoreType() { return keystoreType; }

        @Override
        public String subject() { return subject; }

        @Override
        public LicenseValidation validation() {
            final LicenseValidation second = new TrueLicenseValidation();
            return validation
                    .map(first -> validationComposition.compose(first, second))
                    .orElse(second);
        }

        @Override
        public VendorLicenseManagerBuilder vendor() { return new VendorTrueLicenseManagerBuilder(); }

        class ConsumerTrueLicenseManagerBuilder
        extends TrueLicenseManagerBuilder<ConsumerTrueLicenseManagerBuilder>
        implements ConsumerLicenseManagerBuilder {

            @Override
            public ConsumerLicenseManager build() {
                final TrueLicenseManagementParameters p = new TrueLicenseManagementParameters(this);
                return parent.isPresent()
                        ? p.new ChainedTrueLicenseManager()
                        : p.new CachingTrueLicenseManager();
            }

            @Override
            public ConsumerTrueLicenseManagerBuilder up() { throw new UnsupportedOperationException(); }

            @Override
            public ParentConsumerTrueLicenseManagerBuilder parent() {
                return new ParentConsumerTrueLicenseManagerBuilder();
            }

            final class ParentConsumerTrueLicenseManagerBuilder
            extends ConsumerTrueLicenseManagerBuilder {

                @Override
                public ConsumerTrueLicenseManagerBuilder up() {
                    return ConsumerTrueLicenseManagerBuilder.this.parent(build());
                }
            }
        }

        final class VendorTrueLicenseManagerBuilder
        extends TrueLicenseManagerBuilder<VendorTrueLicenseManagerBuilder>
        implements VendorLicenseManagerBuilder {

            @Override
            public VendorLicenseManager build() {
                return new TrueLicenseManagementParameters(this).new TrueLicenseManager();
            }
        }

        abstract class TrueLicenseManagerBuilder<This extends TrueLicenseManagerBuilder<This> & Builder<?>> {

            Optional<Authentication> authentication = Optional.empty();
            Optional<Transformation> encryption = Optional.empty();
            int ftpDays;
            Optional<ConsumerLicenseManager> parent = Optional.empty();
            Optional<Store> store = Optional.empty();

            public final This authentication(final Authentication authentication) {
                this.authentication = Optional.ofNullable(authentication);
                return (This) this;
            }

            public final TrueEncryptionBuilder encryption() { return new TrueEncryptionBuilder(); }

            public final This encryption(final Transformation encryption) {
                this.encryption = Optional.ofNullable(encryption);
                return (This) this;
            }

            public final This ftpDays(final int ftpDays) {
                this.ftpDays = ftpDays;
                return (This) this;
            }

            public final TrueAuthenticationBuilder authentication() { return new TrueAuthenticationBuilder(); }

            public final This parent(final ConsumerLicenseManager parent) {
                this.parent = Optional.ofNullable(parent);
                return (This) this;
            }

            public final This storeIn(final Store store) {
                this.store = Optional.ofNullable(store);
                return (This) this;
            }

            public final This storeInPath(Path path) { return storeIn(pathStore(path)); }

            public final This storeInSystemPreferences(Class<?> classInPackage) {
                return storeIn(systemPreferencesStore(classInPackage, subject()));
            }

            public final This storeInUserPreferences(Class<?> classInPackage) {
                return storeIn(userPreferencesStore(classInPackage, subject()));
            }

            final class TrueAuthenticationBuilder implements Builder<Authentication>, AuthenticationBuilder<This> {

                Optional<String> algorithm = Optional.empty();
                Optional<String> alias = Optional.empty();
                Optional<PasswordProtection> keyProtection = Optional.empty();
                Optional<Socket<InputStream>> source = Optional.empty();
                Optional<PasswordProtection> storeProtection = Optional.empty();
                Optional<String> storeType = Optional.empty();

                @Override
                public This up() { return authentication(build()); }

                @Override
                public TrueAuthenticationBuilder algorithm(final String algorithm) {
                    this.algorithm = Optional.ofNullable(algorithm);
                    return this;
                }

                @Override
                public TrueAuthenticationBuilder alias(final String alias) {
                    this.alias = Optional.ofNullable(alias);
                    return this;
                }

                @Override
                public Authentication build() {
                    return authenticationFactory().apply(new TrueAuthenticationParameters(this));
                }

                @Override
                public TrueAuthenticationBuilder keyProtection(final PasswordProtection keyProtection) {
                    this.keyProtection = Optional.ofNullable(keyProtection);
                    return this;
                }

                @Override
                public TrueAuthenticationBuilder loadFrom(final Socket<InputStream> input) {
                    this.source = Optional.ofNullable(input);
                    return this;
                }

                @Override
                public TrueAuthenticationBuilder loadFromResource(String name) { return loadFrom(resource(name)); }

                @Override
                public TrueAuthenticationBuilder storeProtection(final PasswordProtection storeProtection) {
                    this.storeProtection = Optional.ofNullable(storeProtection);
                    return this;
                }

                @Override
                public TrueAuthenticationBuilder storeType(final String storeType) {
                    this.storeType = Optional.ofNullable(storeType);
                    return this;
                }
            }

            final class TrueEncryptionBuilder implements Builder<Transformation>, EncryptionBuilder<This> {

                Optional<String> algorithm = Optional.empty();
                Optional<PasswordProtection> protection = Optional.empty();

                @Override
                public This up() { return encryption(build()); }

                @Override
                public TrueEncryptionBuilder algorithm(final String algorithm) {
                    this.algorithm = Optional.ofNullable(algorithm);
                    return this;
                }

                @Override
                public Transformation build() {
                    return encryptionFactory().apply(new TrueEncryptionParameters(this));
                }

                @Override
                public TrueEncryptionBuilder protection(final PasswordProtection protection) {
                    this.protection = Optional.ofNullable(protection);
                    return this;
                }
            }
        }

        final class TrueAuthenticationParameters implements AuthenticationParameters {

            final Optional<String> algorithm;
            final String alias;
            final Optional<PasswordProtection> keyProtection;
            final Optional<Socket<InputStream>> source;
            final PasswordProtection storeProtection;
            final Optional<String> storeType;

            TrueAuthenticationParameters(final TrueLicenseManagerBuilder<?>.TrueAuthenticationBuilder b) {
                this.algorithm = b.algorithm;
                this.alias = b.alias.get();
                this.keyProtection = b.keyProtection;
                this.source = b.source;
                this.storeProtection = b.storeProtection.get();
                this.storeType = b.storeType;
            }

            @Override
            public String alias() { return alias; }

            @Override
            public PasswordProtection keyProtection() {
                return keyProtection
                        .map(CheckedPasswordProtection::new)
                        .orElseGet(() -> new CheckedPasswordProtection(storeProtection));
            }

            @Override
            public Optional<String> algorithm() { return algorithm; }

            @Override
            public Optional<Socket<InputStream>> source() { return source; }

            @Override
            public PasswordProtection storeProtection() { return new CheckedPasswordProtection(storeProtection); }

            @Override
            public String storeType() { return storeType.orElseGet(TrueLicenseManagementContext.this::keystoreType); }
        }

        final class TrueEncryptionParameters implements EncryptionParameters {

            final Optional<String> algorithm;
            final PasswordProtection protection;

            TrueEncryptionParameters(final TrueLicenseManagerBuilder<?>.TrueEncryptionBuilder b) {
                this.algorithm = b.algorithm;
                this.protection = b.protection.get();
            }

            @Override
            public String algorithm() {
                return algorithm.orElseGet(TrueLicenseManagementContext.this::encryptionAlgorithm);
            }

            @Override
            public PasswordProtection protection() { return new CheckedPasswordProtection(protection); }
        }

        final class CheckedPasswordProtection implements PasswordProtection {

            final PasswordProtection protection;

            CheckedPasswordProtection(final PasswordProtection protection) { this.protection = protection; }

            @Override
            public Password password(final PasswordUsage usage) throws Exception {
                if (usage.equals(PasswordUsage.WRITE)) { // checks null
                    passwordPolicy().check(protection);
                }
                return protection.password(usage);
            }
        }

        final class TrueLicenseManagementParameters
        implements LicenseInitializationProvider, LicenseManagementParameters {

            final Authentication authentication;
            final Optional<Transformation> encryption;
            final int ftpDays;
            final Optional<ConsumerLicenseManager> parent;
            final Optional<Store> store;

            TrueLicenseManagementParameters(final TrueLicenseManagerBuilder<?> b) {
                this.authentication = b.authentication.get();
                this.encryption = b.encryption;
                this.ftpDays = b.ftpDays;
                this.parent = b.parent;
                this.store = b.store;
            }

            @Override
            public Authentication authentication() { return authentication; }

            @Override
            public Transformation encryption() {
                return encryption.orElseGet(() -> parent().parameters().encryption());
            }

            @Override
            public LicenseInitialization initialization() {
                final LicenseInitialization initialization = TrueLicenseManagementContext.this.initialization();
                if (0 != ftpDays) {
                    return bean -> {
                        initialization.initialize(bean);
                        final Calendar cal = getInstance();
                        cal.setTime(bean.getIssued());
                        bean.setNotBefore(cal.getTime()); // not before issued
                        cal.add(DATE, ftpDays); // FTP countdown starts NOW
                        bean.setNotAfter(cal.getTime());
                    };
                } else {
                    return initialization;
                }
            }

            ConsumerLicenseManager parent() { return parent.get(); }

            Store store() { return store.get();}

            Transformation compressionThenEncryption() { return compression().andThen(encryption()); }

            final class ChainedTrueLicenseManager extends CachingTrueLicenseManager {

                volatile Optional<Boolean> canGenerateLicenseKeys = Optional.empty();

                @Override
                public void install(Source source) throws LicenseManagementException {
                    try {
                        parent().install(source);
                    } catch (final LicenseManagementException first) {
                        if (canGenerateLicenseKeys()) {
                            throw first;
                        }
                        super.install(source);
                    }
                }

                @Override
                public License load() throws LicenseManagementException {
                    try {
                        return parent().load();
                    } catch (final LicenseManagementException first) {
                        try {
                            return super.load(); // uses store()
                        } catch (final LicenseManagementException second) {
                            synchronized (store()) {
                                try {
                                    return super.load(); // repeat
                                } catch (final LicenseManagementException third) {
                                    return generateIffNewFtp(third).license(); // uses store(), too
                                }
                            }
                        }
                    }
                }

                @Override
                public void verify() throws LicenseManagementException {
                    try {
                        parent().verify();
                    } catch (final LicenseManagementException first) {
                        try {
                            super.verify(); // uses store()
                        } catch (final LicenseManagementException second) {
                            synchronized (store()) {
                                try {
                                    super.verify(); // repeat
                                } catch (final LicenseManagementException third) {
                                    generateIffNewFtp(third); // uses store(), too
                                }
                            }
                        }
                    }
                }

                @Override
                public void uninstall() throws LicenseManagementException {
                    try {
                        parent().uninstall();
                    } catch (final LicenseManagementException first) {
                        if (canGenerateLicenseKeys()) {
                            throw first;
                        }
                        super.uninstall();
                    }
                }

                boolean canGenerateLicenseKeys() {
                    if (!canGenerateLicenseKeys.isPresent()) {
                        synchronized (this) {
                            if (!canGenerateLicenseKeys.isPresent()) {
                                try {
                                    // Test encoding a new license key to /dev/null .
                                    super.generateKeyFrom(license()).saveTo(memoryStore());
                                    canGenerateLicenseKeys = Optional.of(Boolean.TRUE);
                                } catch (LicenseManagementException ignored) {
                                    canGenerateLicenseKeys = Optional.of(Boolean.FALSE);
                                }
                            }
                        }
                    }
                    return canGenerateLicenseKeys.get();
                }

                LicenseKeyGenerator generateIffNewFtp(final LicenseManagementException e) throws LicenseManagementException {
                    if (!canGenerateLicenseKeys()) {
                        throw e;
                    }
                    final Store store = store();
                    if (checked(store::exists)) {
                        throw e;
                    }
                    return super.generateKeyFrom(license()).saveTo(store);
                }
            }

            class CachingTrueLicenseManager extends TrueLicenseManager {

                // These volatile fields get initialized by applying a pure function which
                // takes the immutable value of the store() property as its single argument.
                // So some concurrent threads may safely interleave when initializing these
                // fields without creating a racing condition and thus it's not generally
                // required to synchronize access to them.
                volatile Cache<Source, Decoder> cachedDecoder = new Cache<>();
                volatile Cache<Source, License> cachedLicense = new Cache<>();

                @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
                @Override
                public void install(final Source source) throws LicenseManagementException {
                    final Optional<Source> optSource = Optional.of(source);
                    final Store store = store();
                    final Optional<Source> optStore = Optional.of(store);
                    synchronized (store) {
                        super.install(source);

                        // As a side effect of the license key installation, the cached
                        // artifactory and license get associated to the source unless this
                        // is a re-installation from an equal source or the cached objects
                        // have already been obsoleted by a time-out, that is, if the cache
                        // period is equal or close to zero.
                        assert cachedDecoder.hasKey(optSource) ||
                                cachedDecoder.hasKey(optStore) ||
                                cachedDecoder.obsolete();
                        assert cachedLicense.hasKey(optSource) ||
                                cachedLicense.hasKey(optStore) ||
                                cachedLicense.obsolete();

                        // Update the association of the cached artifactory and license to
                        // the store.
                        cachedDecoder = cachedDecoder.key(optStore);
                        cachedLicense = cachedLicense.key(optStore);
                    }
                }

                @Override
                public void uninstall() throws LicenseManagementException {
                    final Cache<Source, Decoder> cachedDecoder = new Cache<>();
                    final Cache<Source, License> cachedLicense = new Cache<>();
                    synchronized (store()) {
                        super.uninstall();
                        this.cachedDecoder = cachedDecoder;
                        this.cachedLicense = cachedLicense;
                    }
                }

                @Override
                void validate(final Source source) throws Exception {
                    final Optional<Source> optSource = Optional.of(source);
                    Optional<License> optLicense = cachedLicense.map(optSource);
                    if (!optLicense.isPresent()) {
                        optLicense = Optional.of(decodeLicense(source));
                        cachedLicense = new Cache<>(optSource, optLicense, cachePeriodMillis());
                    }
                    validation().validate(optLicense.get());
                }

                @Override
                Decoder authenticate(final Source source) throws Exception {
                    final Optional<Source> optSource = Optional.of(source);
                    Optional<Decoder> optDecoder = cachedDecoder.map(optSource);
                    if (!optDecoder.isPresent()) {
                        optDecoder = Optional.of(super.authenticate(source));
                        cachedDecoder = new Cache<>(optSource, optDecoder, cachePeriodMillis());
                    }
                    return optDecoder.get();
                }
            }

            /**
             * A basic license manager.
             * This class is immutable.
             * <p>
             * Unless stated otherwise, all no-argument methods need to return consistent
             * objects so that caching them is not required.
             * A returned object is considered to be consistent if it compares
             * {@linkplain Object#equals(Object) equal} or at least behaves identical to
             * any previously returned object.
             *
             * @author Christian Schlichtherle
             */
            class TrueLicenseManager
            implements ConsumerLicenseManager, VendorLicenseManager {

                @Override
                public LicenseKeyGenerator generateKeyFrom(final License bean) throws LicenseManagementException {

                    class TrueLicenseKeyGenerator implements LicenseKeyGenerator {

                        final Object model = repositoryContext().model();
                        Decoder decoder;

                        @Override
                        public License license() throws LicenseManagementException {
                            return checked(() -> decoder().decode(License.class));
                        }

                        @Override
                        public LicenseKeyGenerator saveTo(final Sink sink) throws LicenseManagementException {
                            checked(() -> {
                                codec().encoder(sink.map(compressionThenEncryption())).encode(model());
                                return null;
                            });
                            return this;
                        }

                        Decoder decoder() throws Exception {
                            init();
                            return decoder;
                        }

                        Object model() throws Exception {
                            init();
                            return model;
                        }

                        synchronized void init() throws Exception {
                            if (null == decoder) {
                                decoder = authentication()
                                        .sign(repositoryContext().controller(model, codec()), validatedBean());
                            }
                        }

                        License validatedBean() throws Exception {
                            final License duplicate = initializedBean();
                            validation().validate(duplicate);
                            return duplicate;
                        }

                        License initializedBean() throws Exception {
                            final License duplicate = duplicatedBean();
                            initialization().initialize(duplicate);
                            return duplicate;
                        }

                        License duplicatedBean() throws Exception {
                            return memoryStore().connect(codec()).clone(bean);
                        }
                    }

                    return checked(() -> {
                        authorization().clearGenerate(TrueLicenseManager.this);
                        return new TrueLicenseKeyGenerator();
                    });
                }

                @Override
                public void install(final Source source) throws LicenseManagementException {
                    checked(() -> {
                        authorization().clearInstall(TrueLicenseManager.this);
                        decodeLicense(source); // checks digital signature
                        copy(source, store());
                        return null;
                    });
                }

                @Override
                public License load() throws LicenseManagementException {
                    return checked(() -> {
                        authorization().clearLoad(TrueLicenseManager.this);
                        return decodeLicense(store());
                    });
                }

                @Override
                public void verify() throws LicenseManagementException {
                    checked(() -> {
                        authorization().clearVerify(TrueLicenseManager.this);
                        validate(store());
                        return null;
                    });
                }

                @Override
                public void uninstall() throws LicenseManagementException {
                    checked(() -> {
                        authorization().clearUninstall(TrueLicenseManager.this);
                        final Store store1 = store();
                        // #TRUELICENSE-81: A consumer license manager must
                        // authenticate the installed license key before uninstalling
                        // it.
                        authenticate(store1);
                        store1.delete();
                        return null;
                    });
                }

                //
                // License consumer functions:
                //

                void validate(Source source) throws Exception { validation().validate(decodeLicense(source)); }

                License decodeLicense(Source source) throws Exception {
                    return authenticate(source).decode(License.class);
                }

                Decoder authenticate(Source source) throws Exception {
                    return authentication().verify(repositoryController(source));
                }

                RepositoryController repositoryController(Source source) throws Exception {
                    return repositoryContext().controller(repositoryModel(source), codec());
                }

                Object repositoryModel(Source source) throws Exception {
                    return codec()
                            .decoder(decryptedAndDecompressedSource(source))
                            .decode(repositoryContext().model().getClass());
                }

                Source decryptedAndDecompressedSource(Source source) { return source.map(compressionThenEncryption()); }

                //
                // Property/factory functions:
                //

                @Override
                public final LicenseManagementContext context() { return TrueLicenseManagementContext.this; }

                @Override
                public final TrueLicenseManagementParameters parameters() {
                    return TrueLicenseManagementParameters.this;
                }

                @Override
                public UncheckedTrueLicenseManager unchecked() { return new UncheckedTrueLicenseManager(); }

                private class UncheckedTrueLicenseManager
                        implements UncheckedVendorLicenseManager, UncheckedConsumerLicenseManager {

                        @Override
                    public UncheckedLicenseKeyGenerator generateKeyFrom(final License bean)
                            throws UncheckedLicenseManagementException {
                        return TrueLicenseApplicationContext.unchecked(() -> new UncheckedLicenseKeyGenerator() {
                            final LicenseKeyGenerator generator = checked().generateKeyFrom(bean);

                            @Override
                            public License license() throws UncheckedLicenseManagementException {
                                return TrueLicenseApplicationContext.unchecked(generator::license);
                            }

                            @Override
                            public UncheckedLicenseKeyGenerator saveTo(Sink sink) throws UncheckedLicenseManagementException {
                                TrueLicenseApplicationContext.unchecked(() -> generator.saveTo(sink));
                                return this;
                            }
                        });
                    }

                    @Override
                    public void install(final Source source) throws UncheckedLicenseManagementException {
                        TrueLicenseApplicationContext.unchecked(() -> {
                            checked().install(source);
                            return null;
                        });
                    }

                    @Override
                    public License load() throws UncheckedLicenseManagementException {
                        return TrueLicenseApplicationContext.unchecked(checked()::load);
                    }

                    @Override
                    public void verify() throws UncheckedLicenseManagementException {
                        TrueLicenseApplicationContext.unchecked(() -> {
                            checked().verify();
                            return null;
                        });
                    }

                    @Override
                    public void uninstall() throws UncheckedLicenseManagementException {
                        TrueLicenseApplicationContext.unchecked(() -> {
                            checked().uninstall();
                            return null;
                        });
                    }

                    @Override
                    public LicenseManagementContext context() { return checked().context(); }

                    @Override
                    public LicenseManagementParameters parameters() { return checked().parameters(); }

                    @Override
                    public TrueLicenseManager checked() { return TrueLicenseManager.this; }
                }
            }
        }

        /**
         * A basic license initialization.
         * This class is immutable.
         * <p>
         * This implementation of the {@link LicenseInitialization} interface
         * initializes the license
         * {@linkplain License#getConsumerType consumer type},
         * {@linkplain License#getHolder holder},
         * {@linkplain License#getIssued issue date/time},
         * {@linkplain License#getIssuer issuer} and
         * {@linkplain License#getSubject subject}
         * unless these properties are respectively set already.
         * <p>
         * Unless stated otherwise, all no-argument methods need to return consistent
         * objects so that caching them is not required.
         * A returned object is considered to be consistent if it compares
         * {@linkplain Object#equals(Object) equal} or at least behaves identical to
         * any previously returned object.
         */
        final class TrueLicenseInitialization implements LicenseInitialization {

            @Obfuscate
            static final String DEFAULT_CONSUMER_TYPE = "User";

            /** The canonical name prefix for X.500 principals. */
            @Obfuscate
            static final String CN_PREFIX = "CN=";

            @Override
            public void initialize(final License bean) {
                if (null == bean.getConsumerType()) {
                    bean.setConsumerType(DEFAULT_CONSUMER_TYPE);
                }
                if (null == bean.getHolder()) {
                    bean.setHolder(new X500Principal(CN_PREFIX + message(UNKNOWN)));
                }
                if (null == bean.getIssued()) {
                    bean.setIssued(now()); // don't trust the system clock!
                }
                if (null == bean.getIssuer()) {
                    bean.setIssuer(new X500Principal(CN_PREFIX + subject()));
                }
                if (null == bean.getSubject()) {
                    bean.setSubject(subject());
                }
            }
        }

        /**
         * A basic license validation.
         * This class is immutable.
         * <p>
         * This implementation of the {@link LicenseValidation} interface validates the
         * license
         * {@linkplain License#getConsumerAmount consumer amount},
         * {@linkplain License#getConsumerType consumer type},
         * {@linkplain License#getHolder holder},
         * {@linkplain License#getIssued issue date/time},
         * {@linkplain License#getIssuer issuer},
         * {@linkplain License#getNotAfter not after date/time} (if set),
         * {@linkplain License#getNotBefore not before date/time} (if set) and
         * {@linkplain License#getSubject subject}.
         * <p>
         * Unless stated otherwise, all no-argument methods need to return consistent
         * objects so that caching them is not required.
         * A returned object is considered to be consistent if it compares
         * {@linkplain Object#equals(Object) equal} or at least behaves identical to
         * any previously returned object.
         */
        final class TrueLicenseValidation implements LicenseValidation {

            @Override
            public void validate(final License bean) throws LicenseValidationException {
                if (0 >= bean.getConsumerAmount()) {
                    throw new LicenseValidationException(message(CONSUMER_AMOUNT_IS_NOT_POSITIVE, bean.getConsumerAmount()));
                }
                if (null == bean.getConsumerType()) {
                    throw new LicenseValidationException(message(CONSUMER_TYPE_IS_NULL));
                }
                if (null == bean.getHolder()) {
                    throw new LicenseValidationException(message(HOLDER_IS_NULL));
                }
                if (null == bean.getIssued()) {
                    throw new LicenseValidationException(message(ISSUED_IS_NULL));
                }
                if (null == bean.getIssuer()) {
                    throw new LicenseValidationException(message(ISSUER_IS_NULL));
                }
                final Date now = now(); // don't trust the system clock!
                final Date notAfter = bean.getNotAfter();
                if (null != notAfter && now.after(notAfter)) {
                    throw new LicenseValidationException(message(LICENSE_HAS_EXPIRED, notAfter));
                }
                final Date notBefore = bean.getNotBefore();
                if (null != notBefore && now.before(notBefore)) {
                    throw new LicenseValidationException(message(LICENSE_IS_NOT_YET_VALID, notBefore));
                }
                if (!subject().equals(bean.getSubject())) {
                    throw new LicenseValidationException(message(INVALID_SUBJECT, bean.getSubject(), subject()));
                }
            }
        }
    }
}
